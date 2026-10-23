package ro.greentween.copaculmeu.data.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;
import ro.greentween.copaculmeu.data.enums.UserRole;
import ro.greentween.copaculmeu.data.enums.UserStatus;
import ro.greentween.copaculmeu.data.model.User;
import tech.petrepopescu.common.data.filter.JPAFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UserFilter extends JPAFilter<User> {
    private UUID id;
    private String email;
    private UserRole role;
    private UserStatus status;

    @Override
    public Predicate getPredicate(CriteriaBuilder criteriaBuilder, Root<User> root) {
        List<Predicate> predicates = new ArrayList<>();
        if (id != null) {
            predicates.add(equals(criteriaBuilder, root, "id", id));
        }
        if (email != null) {
            predicates.add(equalsIgnoreCase(criteriaBuilder, root, "email", email.trim()));
        }
        if (role != null) {
            predicates.add(equals(criteriaBuilder, root, "role", role));
        }
        if (status != null) {
            predicates.add(equals(criteriaBuilder, root, "status", status));
        }
        return andPredicateBuilder(criteriaBuilder, predicates.toArray(new Predicate[0]));
    }
}
