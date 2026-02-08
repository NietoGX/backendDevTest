package com.globant.david.msglobantproducts.testsupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests.
 * Provides common configuration and utilities for integration tests including:
 * - Spring Boot test context
 * - Test profile activation
 * - Cache management utilities
 * <p>
 * Integration tests should extend this class to inherit these configurations.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Integration Tests")
public abstract class IntegrationTest {

    @Autowired(required = false)
    protected CacheManager cacheManager;

    @BeforeEach
    void clearAllCaches() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
    }
}
