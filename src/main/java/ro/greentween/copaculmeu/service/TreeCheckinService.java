package ro.greentween.copaculmeu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ro.greentween.copaculmeu.data.dao.TreeCheckinDao;
import ro.greentween.copaculmeu.data.dao.TreeDao;
import ro.greentween.copaculmeu.data.dao.UserDao;
import ro.greentween.copaculmeu.data.dto.CheckinStatusResponse;
import ro.greentween.copaculmeu.data.dto.TreeCheckinRequest;
import ro.greentween.copaculmeu.data.dto.TreeCheckinResponse;
import ro.greentween.copaculmeu.data.enums.TreeStatus;
import ro.greentween.copaculmeu.data.model.Tree;
import ro.greentween.copaculmeu.data.model.TreeCheckin;
import ro.greentween.copaculmeu.data.model.User;
import ro.greentween.copaculmeu.exception.ResourceNotFoundException;
import ro.greentween.copaculmeu.util.GeoUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TreeCheckinService {

    private final TreeDao treeDao;
    private final UserDao userDao;
    private final TreeCheckinDao treeCheckinDao;
    private final SpeciesService speciesService;
    private final TreeCacheEvictionService treeCacheEvictionService;

    public static final double MAX_VALID_DEVIATION_METERS = 2.0;
    public static final int MIN_PROPOSALS_FOR_SPECIES_UPDATE = 5;
    public static final int GPS_AVERAGING_PAST_MONTHS = 3;
    public static final int CHECKIN_COOLDOWN_HOURS = 24;

    @Transactional
    public TreeCheckinResponse checkIn(Long treeId, TreeCheckinRequest request, UUID userId) {
        Tree tree = treeDao.find(treeId);
        if (tree == null) {
            throw new ResourceNotFoundException("Tree with id " + treeId + " not found");
        }

        if (tree.getStatus() != TreeStatus.APPROVED) {
            throw new IllegalStateException("Check-in is only permitted on approved trees. Current tree status: " + tree.getStatus());
        }

        User user = userDao.find(userId);
        if (user == null) {
            throw new IllegalStateException("User with id " + userId + " not found");
        }

        // 24-hour cooldown check: user can only check-in on the same tree once every 24 hours
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        Optional<TreeCheckin> lastCheckinOpt = treeCheckinDao.findLatestValidCheckinForUserAndTree(treeId, userId);
        if (lastCheckinOpt.isPresent()) {
            TreeCheckin lastCheckin = lastCheckinOpt.get();
            LocalDateTime nextAllowed = lastCheckin.getCreatedAt().plusHours(CHECKIN_COOLDOWN_HOURS);
            if (now.isBefore(nextAllowed)) {
                Duration remaining = Duration.between(now, nextAllowed);
                long hours = remaining.toHours();
                long minutes = remaining.toMinutes() % 60;
                String waitTime = hours > 0 ? (hours + "h " + minutes + "m") : (minutes + "m");
                throw new IllegalArgumentException(
                        "You can only check-in on this tree once every 24 hours. Next check-in available in " + waitTime + "."
                );
            }
        }

        if (StringUtils.hasText(request.getProposedSpecies())) {
            speciesService.validateSpecies(request.getProposedSpecies());
        }

        // Calculate deviation from the tree's original coordinates
        double deviation = GeoUtils.calculateDistanceMeters(
                tree.getOriginalLatitude(),
                tree.getOriginalLongitude(),
                request.getLatitude(),
                request.getLongitude()
        );

        boolean isValid = deviation <= MAX_VALID_DEVIATION_METERS;

        TreeCheckin checkin = TreeCheckin.builder()
                .tree(tree)
                .user(user)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .deviationMeters(deviation)
                .valid(isValid)
                .wateringLevel(request.getWateringLevel())
                .watered(request.getWatered())
                .proposedSpecies(StringUtils.hasText(request.getProposedSpecies()) ? request.getProposedSpecies().trim() : null)
                .healthLevel(request.getHealthLevel())
                .height(request.getHeight())
                .age(request.getAge())
                .trunkCircumference(request.getTrunkCircumference())
                .locationDescription(StringUtils.hasText(request.getLocationDescription()) ? request.getLocationDescription().trim() : null)
                .notes(StringUtils.hasText(request.getNotes()) ? request.getNotes().trim() : null)
                .build();

        TreeCheckin savedCheckin = treeCheckinDao.persist(checkin);
        log.info("Check-in recorded for tree {} by user {}: valid={}, deviation={}m",
                treeId, userId, isValid, String.format(Locale.ROOT, "%.2f", deviation));

        if (isValid) {
            // 1. Update watering level and last watered timestamp if watered
            tree.setWateringLevel(request.getWateringLevel());
            if (Boolean.TRUE.equals(request.getWatered())) {
                tree.setLastWateredAt(LocalDateTime.now(ZoneId.of("UTC")));
            }

            // 2. Update optional stats if provided
            if (request.getHealthLevel() != null) {
                tree.setHealthLevel(request.getHealthLevel());
            }
            if (request.getHeight() != null) {
                tree.setHeight(request.getHeight());
            }
            if (request.getAge() != null) {
                tree.setAge(request.getAge());
            }
            if (request.getTrunkCircumference() != null) {
                tree.setTrunkCircumference(request.getTrunkCircumference());
            }
            if (StringUtils.hasText(request.getLocationDescription())) {
                tree.setLocationDescription(request.getLocationDescription().trim());
            }
            if (StringUtils.hasText(request.getNotes())) {
                tree.setNotes(request.getNotes().trim());
            }

            // 3. Recalculate GPS coordinates from valid check-ins in the past 3 months
            recalculateGpsCoordinates(tree);

            // 4. Evaluate species proposals
            evaluateSpeciesProposals(tree);

            treeDao.persist(tree);
            treeCacheEvictionService.evictTreeAndAreaCaches(treeId);
        }

        return TreeCheckinResponse.fromEntity(savedCheckin);
    }

    private void recalculateGpsCoordinates(Tree tree) {
        LocalDateTime pastThreeMonths = LocalDateTime.now(ZoneId.of("UTC")).minusMonths(GPS_AVERAGING_PAST_MONTHS);
        List<TreeCheckin> recentValidCheckins = treeCheckinDao.findValidCheckinsForTreeSince(tree.getId(), pastThreeMonths);

        if (!recentValidCheckins.isEmpty()) {
            double avgLat = recentValidCheckins.stream()
                    .mapToDouble(TreeCheckin::getLatitude)
                    .average()
                    .orElse(tree.getLatitude());

            double avgLng = recentValidCheckins.stream()
                    .mapToDouble(TreeCheckin::getLongitude)
                    .average()
                    .orElse(tree.getLongitude());

            tree.setLatitude(avgLat);
            tree.setLongitude(avgLng);
            log.info("Recalculated GPS coordinates for tree {} based on {} valid check-ins in past 3 months: lat={}, lng={}",
                    tree.getId(), recentValidCheckins.size(), avgLat, avgLng);
        }
    }

    private void evaluateSpeciesProposals(Tree tree) {
        List<TreeCheckin> speciesCheckins = treeCheckinDao.findCheckinsWithProposedSpecies(tree.getId());
        if (speciesCheckins.isEmpty()) {
            return;
        }

        // Group valid check-ins by proposed species (case-insensitive)
        Map<String, List<TreeCheckin>> speciesToCheckins = speciesCheckins.stream()
                .filter(c -> Boolean.TRUE.equals(c.getValid()))
                .filter(c -> StringUtils.hasText(c.getProposedSpecies()))
                .collect(Collectors.groupingBy(
                        c -> c.getProposedSpecies().trim().toLowerCase(Locale.ROOT)
                ));

        if (speciesToCheckins.isEmpty()) {
            return;
        }

        long maxCount = 0;
        String topSpeciesKey = null;
        boolean hasTie = false;

        for (Map.Entry<String, List<TreeCheckin>> entry : speciesToCheckins.entrySet()) {
            long count = entry.getValue().size();
            if (count > maxCount) {
                maxCount = count;
                topSpeciesKey = entry.getKey();
                hasTie = false;
            } else if (count == maxCount) {
                hasTie = true;
            }
        }

        // If there's a tie for top proposals, no species has strictly more proposals than all others
        if (hasTie || topSpeciesKey == null) {
            return;
        }

        String currentSpeciesKey = tree.getSpecies() != null ? tree.getSpecies().trim().toLowerCase(Locale.ROOT) : "";

        // If top species is already the current species, no update needed
        if (topSpeciesKey.equalsIgnoreCase(currentSpeciesKey)) {
            return;
        }

        // Requirement:
        // 1. Multiple users propose a change (at least 2 distinct users)
        // 2. At least 5 proposals for the new species (maxCount >= 5)
        // 3. Strictly more proposals than any other species proposal (!hasTie && maxCount is unique highest)
        if (maxCount >= MIN_PROPOSALS_FOR_SPECIES_UPDATE) {
            List<TreeCheckin> topCheckins = speciesToCheckins.get(topSpeciesKey);
            long distinctUsers = topCheckins.stream()
                    .map(c -> c.getUser().getId())
                    .distinct()
                    .count();

            if (distinctUsers >= 2) {
                String canonicalSpeciesName = topCheckins.get(topCheckins.size() - 1).getProposedSpecies().trim();
                String oldSpecies = tree.getSpecies();
                tree.setSpecies(canonicalSpeciesName);
                log.info("Tree {} species updated from '{}' to '{}' based on {} community proposals from {} users",
                        tree.getId(), oldSpecies, canonicalSpeciesName, maxCount, distinctUsers);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<TreeCheckinResponse> getCheckinsForTree(Long treeId) {
        Tree tree = treeDao.find(treeId);
        if (tree == null) {
            throw new ResourceNotFoundException("Tree with id " + treeId + " not found");
        }

        return treeCheckinDao.findByTreeId(treeId)
                .stream()
                .map(TreeCheckinResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CheckinStatusResponse getCheckinStatus(Long treeId, UUID userId) {
        Tree tree = treeDao.find(treeId);
        if (tree == null) {
            throw new ResourceNotFoundException("Tree with id " + treeId + " not found");
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        Optional<TreeCheckin> lastCheckinOpt = treeCheckinDao.findLatestValidCheckinForUserAndTree(treeId, userId);

        if (lastCheckinOpt.isEmpty()) {
            return CheckinStatusResponse.builder()
                    .canCheckin(true)
                    .treeId(treeId)
                    .remainingCooldownSeconds(0L)
                    .message("Check-in is available")
                    .build();
        }

        TreeCheckin lastCheckin = lastCheckinOpt.get();
        LocalDateTime nextAllowed = lastCheckin.getCreatedAt().plusHours(CHECKIN_COOLDOWN_HOURS);

        if (now.isBefore(nextAllowed)) {
            Duration remaining = Duration.between(now, nextAllowed);
            long seconds = remaining.getSeconds();
            long hours = remaining.toHours();
            long minutes = remaining.toMinutes() % 60;
            String waitTime = hours > 0 ? (hours + "h " + minutes + "m") : (minutes + "m");

            return CheckinStatusResponse.builder()
                    .canCheckin(false)
                    .treeId(treeId)
                    .lastCheckinAt(lastCheckin.getCreatedAt())
                    .nextAvailableCheckinAt(nextAllowed)
                    .remainingCooldownSeconds(seconds)
                    .message("Check-in available in " + waitTime)
                    .build();
        } else {
            return CheckinStatusResponse.builder()
                    .canCheckin(true)
                    .treeId(treeId)
                    .lastCheckinAt(lastCheckin.getCreatedAt())
                    .remainingCooldownSeconds(0L)
                    .message("Check-in is available")
                    .build();
        }
    }
}
