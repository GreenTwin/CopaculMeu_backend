package ro.greentween.copaculmeu.data.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;
import ro.greentween.copaculmeu.data.enums.EmailTemplateType;
import ro.greentween.copaculmeu.data.model.EmailTemplate;
import tech.petrepopescu.common.data.filter.JPAFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class EmailTemplateFilter extends JPAFilter<EmailTemplate> {
    private UUID id;
    private EmailTemplateType name;

    @Override
    public Predicate getPredicate(CriteriaBuilder criteriaBuilder, Root<EmailTemplate> root) {
        List<Predicate> predicates = new ArrayList<>();
        if (id != null) {
            predicates.add(equals(criteriaBuilder, root, "id", id));
        }
        if (name != null) {
            predicates.add(equals(criteriaBuilder, root, "name", name));
        }
        return andPredicateBuilder(criteriaBuilder, predicates.toArray(new Predicate[0]));
    }
}
