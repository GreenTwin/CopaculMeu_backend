package ro.greentween.copaculmeu.data.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;
import ro.greentween.copaculmeu.data.model.UserActivationCode;
import tech.petrepopescu.common.data.filter.JPAFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UserActivationCodeFilter extends JPAFilter<UserActivationCode> {
    private UUID userId;
    private String code;
    private String encryptorVersion;

    @Override
    public Predicate getPredicate(CriteriaBuilder criteriaBuilder, Root<UserActivationCode> root) {
        List<Predicate> predicates = new ArrayList<>();
        if (userId != null) {
            predicates.add(equals(criteriaBuilder, root, "user.id", userId));
        }
        if (code != null) {
            predicates.add(equals(criteriaBuilder, root, "code", code));
        }
        if (encryptorVersion != null) {
            predicates.add(equals(criteriaBuilder, root, "encryptorVersion", encryptorVersion));
        }
        return andPredicateBuilder(criteriaBuilder, predicates.toArray(new Predicate[0]));
    }
}
