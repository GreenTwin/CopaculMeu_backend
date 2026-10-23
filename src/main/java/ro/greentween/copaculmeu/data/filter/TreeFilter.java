package ro.greentween.copaculmeu.data.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;
import ro.greentween.copaculmeu.data.enums.HealthLevel;
import ro.greentween.copaculmeu.data.enums.WateringLevel;
import ro.greentween.copaculmeu.data.model.Tree;
import tech.petrepopescu.common.data.filter.JPAFilter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TreeFilter extends JPAFilter<Tree> {
    private Double minLatitude;
    private Double maxLatitude;
    private Double minLongitude;
    private Double maxLongitude;
    private String species;
    private HealthLevel healthLevel;
    private WateringLevel wateringLevel;

    @Override
    public Predicate getPredicate(CriteriaBuilder criteriaBuilder, Root<Tree> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (minLatitude != null) {
            predicates.add(greaterThanOrEqual(criteriaBuilder, root, "latitude", minLatitude));
        }
        if (maxLatitude != null) {
            predicates.add(lessThanOrEqual(criteriaBuilder, root, "latitude", maxLatitude));
        }
        if (minLongitude != null) {
            predicates.add(greaterThanOrEqual(criteriaBuilder, root, "longitude", minLongitude));
        }
        if (maxLongitude != null) {
            predicates.add(lessThanOrEqual(criteriaBuilder, root, "longitude", maxLongitude));
        }
        if (species != null) {
            predicates.add(equalsIgnoreCase(criteriaBuilder, root, "species", species));
        }
        if (healthLevel != null) {
            predicates.add(equals(criteriaBuilder, root, "healthLevel", healthLevel));
        }
        if (wateringLevel != null) {
            predicates.add(equals(criteriaBuilder, root, "wateringLevel", wateringLevel));
        }

        return andPredicateBuilder(criteriaBuilder, predicates.toArray(new Predicate[0]));
    }
}
