package ro.greentween.copaculmeu.data.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;
import ro.greentween.copaculmeu.data.model.RevokedToken;
import tech.petrepopescu.common.data.filter.JPAFilter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RevokedTokenFilter extends JPAFilter<RevokedToken> {
    private String tokenId;
    private UUID userId;
    private OffsetDateTime expiredBefore;

    @Override
    public Predicate getPredicate(CriteriaBuilder criteriaBuilder, Root<RevokedToken> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (tokenId != null && !tokenId.isBlank()) {
            predicates.add(equals(criteriaBuilder, root, "tokenId", tokenId));
        }

        if (userId != null) {
            predicates.add(equals(criteriaBuilder, root, "user.id", userId));
        }

        if (expiredBefore != null) {
            predicates.add(criteriaBuilder.lessThan(root.get("expiresAt"), expiredBefore));
        }

        return andPredicateBuilder(criteriaBuilder, predicates.toArray(new Predicate[0]));
    }
}
