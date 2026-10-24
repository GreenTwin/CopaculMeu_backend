package ro.greentween.copaculmeu.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(List.of(
                CacheNames.TREES_AREA,
                CacheNames.TREES_NEARBY,
                CacheNames.TREES_ALL,
                CacheNames.TREES_SINGLE,
                CacheNames.TREE_PROPOSALS,
                CacheNames.SPECIES_ALL,
                CacheNames.SPECIES_SINGLE,
                CacheNames.SPECIES_ALLOWED
        ));
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats());
        return cacheManager;
    }
}
