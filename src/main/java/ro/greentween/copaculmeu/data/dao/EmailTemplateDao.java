package ro.greentween.copaculmeu.data.dao;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ro.greentween.copaculmeu.data.enums.EmailTemplateType;
import ro.greentween.copaculmeu.data.filter.EmailTemplateFilter;
import ro.greentween.copaculmeu.data.model.EmailTemplate;
import tech.petrepopescu.common.data.dao.GenericDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EmailTemplateDao extends GenericDao<EmailTemplate> {

    public EmailTemplateDao(EntityManager entityManager) {
        super(EmailTemplate.class, entityManager);
    }

    public Optional<EmailTemplate> findByName(EmailTemplateType name) {
        if (name == null) {
            return Optional.empty();
        }
        EmailTemplateFilter filter = new EmailTemplateFilter();
        filter.setName(name);
        List<EmailTemplate> templates = get(filter);
        return templates.isEmpty() ? Optional.empty() : Optional.of(templates.get(0));
    }

    public Optional<EmailTemplate> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        EmailTemplate template = find(id);
        return Optional.ofNullable(template);
    }

    @Transactional
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM EmailTemplate").executeUpdate();
    }
}
