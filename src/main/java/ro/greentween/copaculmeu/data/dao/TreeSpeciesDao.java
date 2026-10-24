package ro.greentween.copaculmeu.data.dao;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ro.greentween.copaculmeu.data.filter.TreeSpeciesFilter;
import ro.greentween.copaculmeu.data.model.TreeSpecies;
import tech.petrepopescu.common.data.dao.GenericDao;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class TreeSpeciesDao extends GenericDao<TreeSpecies> {

    public TreeSpeciesDao(EntityManager entityManager) {
        super(TreeSpecies.class, entityManager);
    }

    public List<TreeSpecies> findAllOrdered() {
        TreeSpeciesFilter filter = new TreeSpeciesFilter();
        filter.orderBy("name");
        return get(filter);
    }

    public Optional<TreeSpecies> findByNameIgnoreCase(String name) {
        String jpql = "SELECT s FROM TreeSpecies s WHERE LOWER(s.name) = LOWER(:name)";
        return entityManager.createQuery(jpql, TreeSpecies.class)
                .setParameter("name", name.trim())
                .getResultStream()
                .findFirst();
    }

    public boolean isSpeciesKnown(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }

        String normalizedInput = input.trim().toLowerCase(Locale.ROOT);

        // 1. Direct query matching name, scientific_name, or common_name
        String jpql = """
            SELECT COUNT(s.id) FROM TreeSpecies s
            WHERE LOWER(s.name) = :input
               OR LOWER(s.scientificName) = :input
               OR LOWER(s.commonName) = :input
            """;
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("input", normalizedInput)
                .getSingleResult();

        if (count != null && count > 0) {
            return true;
        }

        // 2. Diacritic-agnostic and alias matching
        List<TreeSpecies> all = findAllOrdered();
        String unaccentedInput = removeDiacritics(normalizedInput);

        for (TreeSpecies s : all) {
            if (removeDiacritics(s.getName().toLowerCase(Locale.ROOT)).equals(unaccentedInput)
                    || removeDiacritics(s.getScientificName().toLowerCase(Locale.ROOT)).equals(unaccentedInput)
                    || removeDiacritics(s.getCommonName().toLowerCase(Locale.ROOT)).equals(unaccentedInput)) {
                return true;
            }

            if (s.getAliases() != null) {
                String[] aliasTokens = s.getAliases().split(",");
                for (String alias : aliasTokens) {
                    String cleanAlias = alias.trim().toLowerCase(Locale.ROOT);
                    if (cleanAlias.equals(normalizedInput) || removeDiacritics(cleanAlias).equals(unaccentedInput)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static String removeDiacritics(String str) {
        if (str == null) return "";
        return Normalizer.normalize(str, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    @Transactional
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM TreeSpecies").executeUpdate();
    }
}
