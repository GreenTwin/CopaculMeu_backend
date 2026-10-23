package ro.greentween.copaculmeu.data.dao;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ro.greentween.copaculmeu.data.filter.UserActivationCodeFilter;
import ro.greentween.copaculmeu.data.model.UserActivationCode;
import tech.petrepopescu.common.data.dao.GenericDao;

import java.util.List;
import java.util.UUID;

@Repository
public class UserActivationCodeDao extends GenericDao<UserActivationCode> {

    public UserActivationCodeDao(EntityManager entityManager) {
        super(UserActivationCode.class, entityManager);
    }

    public List<UserActivationCode> findByUserId(UUID userId) {
        UserActivationCodeFilter filter = new UserActivationCodeFilter();
        filter.setUserId(userId);
        return get(filter);
    }

    @Transactional
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM UserActivationCode").executeUpdate();
    }
}
