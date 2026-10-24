package ro.greentween.copaculmeu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.greentween.copaculmeu.data.dao.TreeDao;
import ro.greentween.copaculmeu.data.dao.UserDao;
import ro.greentween.copaculmeu.data.dto.TreeCreateRequest;
import ro.greentween.copaculmeu.data.dto.TreeResponse;
import ro.greentween.copaculmeu.data.enums.TreeStatus;
import ro.greentween.copaculmeu.data.model.Tree;
import ro.greentween.copaculmeu.data.model.User;
import ro.greentween.copaculmeu.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TreeService {

    private final TreeDao treeDao;
    private final UserDao userDao;
    private final SpeciesService speciesService;

    /**
     * Admin direct tree creation (automatically APPROVED)
     */
    @Transactional
    public TreeResponse createTree(TreeCreateRequest request) {
        Tree tree = buildTreeEntity(request);
        tree.setStatus(TreeStatus.APPROVED);

        Tree saved = treeDao.persist(tree);
        log.info("Administrator created approved tree ID {}", saved.getId());
        return TreeResponse.fromEntity(saved);
    }

    /**
     * User tree proposal (created as PENDING_APPROVAL)
     */
    @Transactional
    public TreeResponse proposeTree(TreeCreateRequest request, UUID userId) {
        Tree tree = buildTreeEntity(request);
        tree.setStatus(TreeStatus.PENDING_APPROVAL);

        if (userId != null) {
            User user = userDao.find(userId);
            tree.setProposedBy(user);
        }

        Tree saved = treeDao.persist(tree);
        log.info("User {} proposed tree with ID {}", userId, saved.getId());
        return TreeResponse.fromEntity(saved);
    }

    /**
     * Retrieves all pending tree proposals for administrative review
     */
    @Transactional(readOnly = true)
    public List<TreeResponse> getPendingProposals() {
        return treeDao.findPendingProposals()
                .stream()
                .map(TreeResponse::fromEntity)
                .toList();
    }

    /**
     * Administrator approves a pending tree proposal
     */
    @Transactional
    public TreeResponse approveTreeProposal(Long id, UUID adminUserId) {
        Tree tree = treeDao.find(id);
        if (tree == null) {
            throw new ResourceNotFoundException("Tree with id " + id + " not found");
        }

        if (tree.getStatus() != TreeStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only pending tree proposals can be approved. Current status: " + tree.getStatus());
        }

        if (adminUserId != null) {
            User admin = userDao.find(adminUserId);
            tree.setReviewedBy(admin);
        }

        tree.setStatus(TreeStatus.APPROVED);
        tree.setReviewedAt(LocalDateTime.now(ZoneId.of("UTC")));

        Tree saved = treeDao.persist(tree);
        log.info("Admin {} approved tree ID {}", adminUserId, saved.getId());
        return TreeResponse.fromEntity(saved);
    }

    /**
     * Administrator rejects a pending tree proposal
     */
    @Transactional
    public TreeResponse rejectTreeProposal(Long id, UUID adminUserId, String reason) {
        Tree tree = treeDao.find(id);
        if (tree == null) {
            throw new ResourceNotFoundException("Tree with id " + id + " not found");
        }

        if (tree.getStatus() != TreeStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only pending tree proposals can be rejected. Current status: " + tree.getStatus());
        }

        if (adminUserId != null) {
            User admin = userDao.find(adminUserId);
            tree.setReviewedBy(admin);
        }

        tree.setStatus(TreeStatus.REJECTED);
        tree.setReviewedAt(LocalDateTime.now(ZoneId.of("UTC")));
        tree.setRejectionReason(reason);

        Tree saved = treeDao.persist(tree);
        log.info("Admin {} rejected tree ID {} with reason: {}", adminUserId, saved.getId(), reason);
        return TreeResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<TreeResponse> getTreesInBoundingBox(Double minLat, Double maxLat, Double minLng, Double maxLng) {
        if (minLat == null || maxLat == null || minLng == null || maxLng == null) {
            throw new IllegalArgumentException("All bounding box parameters (minLat, maxLat, minLng, maxLng) are required");
        }
        if (minLat > maxLat) {
            throw new IllegalArgumentException("minLat cannot be greater than maxLat");
        }
        if (minLng > maxLng) {
            throw new IllegalArgumentException("minLng cannot be greater than maxLng");
        }

        return treeDao.findInBoundingBox(minLat, maxLat, minLng, maxLng, TreeStatus.APPROVED)
                .stream()
                .map(TreeResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TreeResponse> getTreesInRadius(Double lat, Double lng, Double radiusMeters) {
        if (lat == null || lng == null || radiusMeters == null) {
            throw new IllegalArgumentException("lat, lng, and radiusMeters are required for radial search");
        }
        if (radiusMeters < 0) {
            throw new IllegalArgumentException("radiusMeters must be positive");
        }

        return treeDao.findTreesWithinRadius(lat, lng, radiusMeters, TreeStatus.APPROVED)
                .stream()
                .map(TreeResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TreeResponse> getAllTrees() {
        return treeDao.findAllApproved()
                .stream()
                .map(TreeResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TreeResponse getTreeById(Long id) {
        Tree tree = treeDao.find(id);
        if (tree == null) {
            throw new ResourceNotFoundException("Tree with id " + id + " not found");
        }
        return TreeResponse.fromEntity(tree);
    }

    private Tree buildTreeEntity(TreeCreateRequest request) {
        speciesService.validateSpecies(request.getSpecies());
        return Tree.builder()
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .species(request.getSpecies().trim())
                .healthLevel(request.getHealthLevel())
                .wateringLevel(request.getWateringLevel())
                .lastWateredAt(request.getLastWateredAt())
                .height(request.getHeight())
                .age(request.getAge())
                .trunkCircumference(request.getTrunkCircumference())
                .locationDescription(request.getLocationDescription() != null ? request.getLocationDescription().trim() : null)
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .build();
    }
}
