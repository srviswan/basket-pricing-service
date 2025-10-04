package com.srviswan.basketpricing.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000) // Max 1000 entries
                .expireAfterWrite(Duration.ofSeconds(30)) // Expire after 30 seconds
                .expireAfterAccess(Duration.ofSeconds(10)) // Expire after 10 seconds of no access
                .recordStats()); // Enable metrics
        return cacheManager;
    }
}
