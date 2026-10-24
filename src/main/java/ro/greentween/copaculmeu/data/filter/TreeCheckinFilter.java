package ro.greentween.copaculmeu.data.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;
import ro.greentween.copaculmeu.data.model.TreeCheckin;
import tech.petrepopescu.common.data.filter.JPAFilter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class TreeCheckinFilter extends JPAFilter<TreeCheckin> {
    private Long treeId;
    private UUID userId;
    private Boolean valid;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;

    @Override
    public Predicate getPredicate(CriteriaBuilder criteriaBuilder, Root<TreeCheckin> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (treeId != null) {
            predicates.add(equals(criteriaBuilder, root, "tree.id", treeId));
        }

        if (userId != null) {
            predicates.add(equals(criteriaBuilder, root, "user.id", userId));
        }

        if (valid != null) {
            predicates.add(equals(criteriaBuilder, root, "valid", valid));
        }

        if (createdAfter != null) {
            predicates.add(greaterThanOrEqual(criteriaBuilder, root, "createdAt", createdAfter));
        }

        if (createdBefore != null) {
            predicates.add(lessThanOrEqual(criteriaBuilder, root, "createdAt", createdBefore));
        }

        return andPredicateBuilder(criteriaBuilder, predicates.toArray(new Predicate[0]));
    }
}
