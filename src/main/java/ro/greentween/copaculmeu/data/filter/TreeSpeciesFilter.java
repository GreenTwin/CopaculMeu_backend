package ro.greentween.copaculmeu.data.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;
import ro.greentween.copaculmeu.data.model.TreeSpecies;
import tech.petrepopescu.common.data.filter.JPAFilter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TreeSpeciesFilter extends JPAFilter<TreeSpecies> {
    private String name;
    private String scientificName;
    private String commonName;

    @Override
    public Predicate getPredicate(CriteriaBuilder criteriaBuilder, Root<TreeSpecies> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (name != null) {
            predicates.add(equals(criteriaBuilder, root, "name", name));
        }

        if (scientificName != null) {
            predicates.add(equals(criteriaBuilder, root, "scientificName", scientificName));
        }

        if (commonName != null) {
            predicates.add(equals(criteriaBuilder, root, "commonName", commonName));
        }

        return andPredicateBuilder(criteriaBuilder, predicates.toArray(new Predicate[0]));
    }
}
