package com.infosys.subsidy.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine-backed in-memory cache configuration for the Admin Dashboard.
 *
 * Cache: "dashboardStats"
 *   - expire-after-write: 60 seconds
 *   - Evicted explicitly by {@code @CacheEvict} on writes that mutate
 *     applications, schemes, milestones, and disbursements.
 */
@Configuration
@EnableCaching
public class DashboardCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("dashboardStats");
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                    .expireAfterWrite(60, TimeUnit.SECONDS)
                    .maximumSize(1)   // Only one stats map is ever cached (no per-user partition)
                    .recordStats()    // Enables cache hit/miss logging via Actuator (if present)
        );
        return cacheManager;
    }
}
