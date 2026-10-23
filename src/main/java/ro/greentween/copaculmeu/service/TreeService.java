package ro.greentween.copaculmeu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.greentween.copaculmeu.data.dao.TreeDao;
import ro.greentween.copaculmeu.data.dto.TreeCreateRequest;
import ro.greentween.copaculmeu.data.dto.TreeResponse;
import ro.greentween.copaculmeu.data.model.Tree;
import ro.greentween.copaculmeu.exception.ResourceNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TreeService {

    private final TreeDao treeDao;

    @Transactional
    public TreeResponse createTree(TreeCreateRequest request) {
        Tree tree = Tree.builder()
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

        Tree saved = treeDao.persist(tree);
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

        return treeDao.findInBoundingBox(minLat, maxLat, minLng, maxLng)
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

        return treeDao.findTreesWithinRadius(lat, lng, radiusMeters)
                .stream()
                .map(TreeResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TreeResponse> getAllTrees() {
        return treeDao.getAll()
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
}
