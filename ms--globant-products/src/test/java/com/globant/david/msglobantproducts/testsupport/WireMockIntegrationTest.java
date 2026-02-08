package com.globant.david.msglobantproducts.testsupport;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Base class for integration tests using WireMock.
 * Provides common configuration and utilities including:
 * - WireMock server lifecycle management
 * - Dynamic property configuration for external API URLs
 * - WebTestClient configuration
 * - Cache management utilities
 * - Helper methods for URL construction
 * <p>
 * Integration tests that require WireMock for external API mocking should extend this class.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
public abstract class WireMockIntegrationTest {

    protected static WireMockServer wireMockServer;
    protected static final int WIREMOCK_PORT = 3001;

    @Autowired(required = false)
    protected CacheManager cacheManager;

    @LocalServerPort
    protected int serverPort;

    protected WebTestClient webTestClient;

    @BeforeAll
    static void startWireMock() {
        if (wireMockServer == null || !wireMockServer.isRunning()) {
            wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(WIREMOCK_PORT));
            wireMockServer.start();
        }
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("external-api.base-url", () -> "http://localhost:" + WIREMOCK_PORT);
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        clearAllCaches();
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + serverPort)
                .build();
    }

    private void clearAllCaches() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
    }

    /**
     * Clears all caches. Useful for tests that need to reset cache state during execution.
     */
    protected void clearCaches() {
        clearAllCaches();
    }

    /**
     * Returns the base URL of the test server.
     */
    protected String baseUrl() {
        return "http://localhost:" + serverPort;
    }

    /**
     * Returns the URL for the product similar endpoint.
     *
     * @param productId the product ID
     * @return the full URL for the similar products endpoint
     */
    protected String productSimilarUrl(String productId) {
        return baseUrl() + "/product/" + productId + "/similar";
    }

    /**
     * Returns the URL for the health endpoint.
     */
    protected String healthUrl() {
        return baseUrl() + "/actuator/health";
    }

    /**
     * Returns the URL for the circuit breakers endpoint.
     */
    protected String circuitBreakersUrl() {
        return baseUrl() + "/actuator/circuitbreakers";
    }
}
