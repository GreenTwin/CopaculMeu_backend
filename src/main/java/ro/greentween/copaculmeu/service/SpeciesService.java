package ro.greentween.copaculmeu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ro.greentween.copaculmeu.config.CacheNames;
import ro.greentween.copaculmeu.data.dao.TreeSpeciesDao;
import ro.greentween.copaculmeu.data.dto.TreeSpeciesCreateRequest;
import ro.greentween.copaculmeu.data.dto.TreeSpeciesResponse;
import ro.greentween.copaculmeu.data.model.TreeSpecies;
import ro.greentween.copaculmeu.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeciesService {

    private final TreeSpeciesDao treeSpeciesDao;
    private final TreeCacheEvictionService treeCacheEvictionService;

    @Cacheable(value = CacheNames.SPECIES_ALL, key = "'all'")
    @Transactional(readOnly = true)
    public List<TreeSpeciesResponse> getAllSpecies() {
        return treeSpeciesDao.findAllOrdered()
                .stream()
                .map(TreeSpeciesResponse::fromEntity)
                .toList();
    }

    @Cacheable(value = CacheNames.SPECIES_SINGLE, key = "#id")
    @Transactional(readOnly = true)
    public TreeSpeciesResponse getSpeciesById(Long id) {
        TreeSpecies species = treeSpeciesDao.find(id);
        if (species == null) {
            throw new ResourceNotFoundException("Tree species with id " + id + " not found");
        }
        return TreeSpeciesResponse.fromEntity(species);
    }

    @Caching(evict = {
            @CacheEvict(value = CacheNames.SPECIES_ALL, allEntries = true),
            @CacheEvict(value = CacheNames.SPECIES_ALLOWED, allEntries = true)
    })
    @Transactional
    public TreeSpeciesResponse addSpecies(TreeSpeciesCreateRequest request) {
        String name = request.getName().trim();
        if (treeSpeciesDao.findByNameIgnoreCase(name).isPresent()) {
            throw new IllegalArgumentException("Tree species with name '" + name + "' already exists");
        }

        TreeSpecies species = TreeSpecies.builder()
                .name(name)
                .scientificName(request.getScientificName().trim())
                .commonName(request.getCommonName().trim())
                .aliases(StringUtils.hasText(request.getAliases()) ? request.getAliases().trim() : null)
                .build();

        TreeSpecies saved = treeSpeciesDao.persist(species);
        treeCacheEvictionService.evictSpeciesCaches();
        log.info("New tree species registered with ID {}: {}", saved.getId(), saved.getName());
        return TreeSpeciesResponse.fromEntity(saved);
    }

    public void validateSpecies(String species) {
        if (!isSpeciesAllowed(species)) {
            throw new IllegalArgumentException(
                    "Invalid species '" + species + "'. Species must be selected from the predefined list or 'Other'."
            );
        }
    }

    @Cacheable(value = CacheNames.SPECIES_ALLOWED, key = "#species.trim().toLowerCase()", condition = "#species != null && !#species.trim().isEmpty()")
    public boolean isSpeciesAllowed(String species) {
        if (!StringUtils.hasText(species)) {
            return false;
        }

        String trimmed = species.trim();

        // 1. Check if it matches "Other" or regional variants
        if (isOtherVariant(trimmed)) {
            return true;
        }

        // 2. Check against database species
        return treeSpeciesDao.isSpeciesKnown(trimmed);
    }

    public static boolean isOtherVariant(String trimmed) {
        if (trimmed.equalsIgnoreCase("Other")
                || trimmed.equalsIgnoreCase("Alta")
                || trimmed.equalsIgnoreCase("Altul")
                || trimmed.equalsIgnoreCase("Altceva")
                || trimmed.equalsIgnoreCase("Other species")
                || trimmed.equalsIgnoreCase("Alta specie")) {
            return true;
        }

        String lower = trimmed.toLowerCase(Locale.ROOT);
        return lower.startsWith("other:")
                || lower.startsWith("other -")
                || lower.startsWith("other (")
                || lower.startsWith("alta:")
                || lower.startsWith("alta -")
                || lower.startsWith("alta (");
    }
}
