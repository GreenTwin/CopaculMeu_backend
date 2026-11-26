package ro.greentween.copaculmeu.data.dao;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ro.greentween.copaculmeu.data.enums.HealthLevel;
import ro.greentween.copaculmeu.data.enums.TreeStatus;
import ro.greentween.copaculmeu.data.enums.WateringLevel;
import ro.greentween.copaculmeu.data.filter.TreeFilter;
import ro.greentween.copaculmeu.data.model.Tree;
import tech.petrepopescu.common.data.dao.GenericDao;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class TreeDao extends GenericDao<Tree> {

    public TreeDao(EntityManager entityManager) {
        super(Tree.class, entityManager);
    }

    public List<Tree> findInBoundingBox(Double minLat, Double maxLat, Double minLng, Double maxLng) {
        return findInBoundingBox(minLat, maxLat, minLng, maxLng, TreeStatus.APPROVED);
    }

    public List<Tree> findInBoundingBox(Double minLat, Double maxLat, Double minLng, Double maxLng, TreeStatus status) {
        TreeFilter filter = new TreeFilter();
        filter.setMinLatitude(minLat);
        filter.setMaxLatitude(maxLat);
        filter.setMinLongitude(minLng);
        filter.setMaxLongitude(maxLng);
        filter.setStatus(status);
        filter.orderBy("id");
        return get(filter);
    }

    public List<Tree> findBySpecies(String species) {
        TreeFilter filter = new TreeFilter();
        filter.setSpecies(species);
        filter.setStatus(TreeStatus.APPROVED);
        return get(filter);
    }

    public List<Tree> findByHealthLevel(HealthLevel healthLevel) {
        TreeFilter filter = new TreeFilter();
        filter.setHealthLevel(healthLevel);
        filter.setStatus(TreeStatus.APPROVED);
        return get(filter);
    }

    public List<Tree> findByWateringLevel(WateringLevel wateringLevel) {
        TreeFilter filter = new TreeFilter();
        filter.setWateringLevel(wateringLevel);
        filter.setStatus(TreeStatus.APPROVED);
        return get(filter);
    }

    public List<Tree> findPendingProposals() {
        TreeFilter filter = new TreeFilter();
        filter.setStatus(TreeStatus.PENDING_APPROVAL);
        filter.orderBy("id");
        return get(filter);
    }

    public List<Tree> findAllApproved() {
        TreeFilter filter = new TreeFilter();
        filter.setStatus(TreeStatus.APPROVED);
        filter.orderBy("id");
        return get(filter);
    }

    public List<Tree> findByStatus(TreeStatus status) {
        TreeFilter filter = new TreeFilter();
        filter.setStatus(status);
        filter.orderBy("id");
        return get(filter);
    }

    public List<Tree> findTreesWithinRadius(Double centerLat, Double centerLng, Double radiusMeters) {
        return findTreesWithinRadius(centerLat, centerLng, radiusMeters, TreeStatus.APPROVED);
    }

    @SuppressWarnings("unchecked")
    public List<Tree> findTreesWithinRadius(Double centerLat, Double centerLng, Double radiusMeters, TreeStatus status) {
        String sql = """
            SELECT * FROM trees t
            WHERE (:status IS NULL OR t.status = :status)
            AND (6371000 * 2 * asin(sqrt(
                power(sin(radians(t.latitude - :centerLat) / 2), 2) +
                cos(radians(:centerLat)) * cos(radians(t.latitude)) *
                power(sin(radians(t.longitude - :centerLng) / 2), 2)
            ))) <= :radiusMeters
            ORDER BY (6371000 * 2 * asin(sqrt(
                power(sin(radians(t.latitude - :centerLat) / 2), 2) +
                cos(radians(:centerLat)) * cos(radians(t.latitude)) *
                power(sin(radians(t.longitude - :centerLng) / 2), 2)
            ))) ASC
            """;
        return entityManager.createNativeQuery(sql, Tree.class)
                .setParameter("centerLat", centerLat)
                .setParameter("centerLng", centerLng)
                .setParameter("radiusMeters", radiusMeters)
                .setParameter("status", status != null ? status.name() : null)
                .getResultList();
    }

    public Map<UUID, Long> countByProposedByUserIds(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = entityManager.createQuery(
                        "SELECT t.proposedBy.id, COUNT(t) FROM Tree t WHERE t.proposedBy.id IN :ids GROUP BY t.proposedBy.id",
                        Object[].class)
                .setParameter("ids", userIds)
                .getResultList();
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    @Transactional
    public void delete(Tree tree) {
        if (tree == null || tree.getId() == null) {
            return;
        }
        Tree managed = entityManager.contains(tree) ? tree : entityManager.find(Tree.class, tree.getId());
        if (managed != null) {
            entityManager.remove(managed);
        }
    }

    @Transactional
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM TreeCheckin").executeUpdate();
        entityManager.createQuery("DELETE FROM Tree").executeUpdate();
    }
}
