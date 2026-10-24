package ro.greentween.copaculmeu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import ro.greentween.copaculmeu.config.CacheNames;

@Slf4j
@Service
@RequiredArgsConstructor
public class TreeCacheEvictionService {

    private final CacheManager cacheManager;

    /**
     * Evicts geographic and collection query caches (bounding box area, nearby radius, and all trees).
     */
    public void evictAreaAndAllCaches() {
        clearCache(CacheNames.TREES_AREA);
        clearCache(CacheNames.TREES_NEARBY);
        clearCache(CacheNames.TREES_ALL);
        log.info("Evicted trees geographic caches: {}, {}, {}",
                CacheNames.TREES_AREA, CacheNames.TREES_NEARBY, CacheNames.TREES_ALL);
    }

    /**
     * Evicts geographic query caches AND the single tree details cache for the given tree ID.
     */
    public void evictTreeAndAreaCaches(Long treeId) {
        clearCache(CacheNames.TREES_AREA);
        clearCache(CacheNames.TREES_NEARBY);
        clearCache(CacheNames.TREES_ALL);
        if (treeId != null) {
            evictKey(CacheNames.TREES_SINGLE, treeId);
        }
        log.info("Evicted tree caches for tree ID {}", treeId);
    }

    /**
     * Evicts the pending tree proposals cache.
     */
    public void evictProposalsCache() {
        clearCache(CacheNames.TREE_PROPOSALS);
        log.info("Evicted pending tree proposals cache: {}", CacheNames.TREE_PROPOSALS);
    }

    /**
     * Evicts predefined species list and validation caches.
     */
    public void evictSpeciesCaches() {
        clearCache(CacheNames.SPECIES_ALL);
        clearCache(CacheNames.SPECIES_ALLOWED);
        log.info("Evicted species caches: {}, {}", CacheNames.SPECIES_ALL, CacheNames.SPECIES_ALLOWED);
    }

    /**
     * Clears all application caches.
     */
    public void clearAllCaches() {
        for (String name : cacheManager.getCacheNames()) {
            clearCache(name);
        }
        log.info("Cleared all application caches");
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private void evictKey(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}
