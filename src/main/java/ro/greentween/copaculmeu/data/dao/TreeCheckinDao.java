package ro.greentween.copaculmeu.data.dao;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ro.greentween.copaculmeu.data.filter.TreeCheckinFilter;
import ro.greentween.copaculmeu.data.model.TreeCheckin;
import tech.petrepopescu.common.data.dao.GenericDao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TreeCheckinDao extends GenericDao<TreeCheckin> {

    public TreeCheckinDao(EntityManager entityManager) {
        super(TreeCheckin.class, entityManager);
    }

    public List<TreeCheckin> findByTreeId(Long treeId) {
        TreeCheckinFilter filter = new TreeCheckinFilter();
        filter.setTreeId(treeId);
        filter.orderBy("-createdAt");
        return get(filter);
    }

    public List<TreeCheckin> findValidCheckinsForTreeSince(Long treeId, LocalDateTime since) {
        TreeCheckinFilter filter = new TreeCheckinFilter();
        filter.setTreeId(treeId);
        filter.setValid(true);
        filter.setCreatedAfter(since);
        filter.orderBy("createdAt");
        return get(filter);
    }

    public Optional<TreeCheckin> findLatestValidCheckinForUserAndTree(Long treeId, UUID userId) {
        String jpql = "SELECT c FROM TreeCheckin c WHERE c.tree.id = :treeId AND c.user.id = :userId AND c.valid = true ORDER BY c.createdAt DESC";
        return entityManager.createQuery(jpql, TreeCheckin.class)
                .setParameter("treeId", treeId)
                .setParameter("userId", userId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    public List<TreeCheckin> findCheckinsWithProposedSpecies(Long treeId) {
        String jpql = "SELECT c FROM TreeCheckin c WHERE c.tree.id = :treeId AND c.valid = true AND c.proposedSpecies IS NOT NULL AND TRIM(c.proposedSpecies) <> '' ORDER BY c.createdAt ASC";
        return entityManager.createQuery(jpql, TreeCheckin.class)
                .setParameter("treeId", treeId)
                .getResultList();
    }

    @Transactional
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM TreeCheckin").executeUpdate();
    }
}
