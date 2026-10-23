package ro.greentween.copaculmeu.data.dao;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ro.greentween.copaculmeu.data.filter.UserFilter;
import ro.greentween.copaculmeu.data.model.User;
import tech.petrepopescu.common.data.dao.GenericDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserDao extends GenericDao<User> {

    public UserDao(EntityManager entityManager) {
        super(User.class, entityManager);
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        UserFilter filter = new UserFilter();
        filter.setEmail(email.trim());
        List<User> users = get(filter);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    @Transactional
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM User").executeUpdate();
    }
}
