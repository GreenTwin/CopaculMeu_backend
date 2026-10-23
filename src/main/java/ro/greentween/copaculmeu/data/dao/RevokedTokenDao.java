package ro.greentween.copaculmeu.data.dao;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ro.greentween.copaculmeu.data.filter.RevokedTokenFilter;
import ro.greentween.copaculmeu.data.model.RevokedToken;
import tech.petrepopescu.common.data.dao.GenericDao;

import java.util.List;
import java.util.UUID;

@Repository
public class RevokedTokenDao extends GenericDao<RevokedToken> {

    public RevokedTokenDao(EntityManager entityManager) {
        super(RevokedToken.class, entityManager);
    }

    public boolean isTokenRevoked(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }
        RevokedTokenFilter filter = new RevokedTokenFilter();
        filter.setTokenId(tokenId);
        return count(filter) > 0;
    }

    public List<RevokedToken> findByUserId(UUID userId) {
        RevokedTokenFilter filter = new RevokedTokenFilter();
        filter.setUserId(userId);
        return get(filter);
    }

    @Transactional
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM RevokedToken").executeUpdate();
    }
}
