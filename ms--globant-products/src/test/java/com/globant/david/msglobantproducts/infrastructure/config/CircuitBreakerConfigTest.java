package com.globant.david.msglobantproducts.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.globant.david.msglobantproducts.testsupport.IntegrationTest;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CircuitBreaker Configuration")
class CircuitBreakerConfigTest extends IntegrationTest {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config =
                io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        .slidingWindowSize(5)
                        .minimumNumberOfCalls(3)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(1))
                        .build();

        circuitBreaker = CircuitBreaker.of("test-circuit", config);
    }

    @Test
    @DisplayName("Circuit breaker from registry should be configured")
    void shouldConfiguredFromRegistry() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("productApiClient");
        assertNotNull(cb);
        assertEquals("productApiClient", cb.getName());
    }

    @Test
    @DisplayName("Circuit breaker should start in CLOSED state")
    void shouldStartInClosedState() {
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    @DisplayName("Circuit breaker config should have correct settings")
    void shouldHaveCorrectConfigSettings() {
        CircuitBreakerConfig config = circuitBreaker.getCircuitBreakerConfig();

        assertEquals(50f, config.getFailureRateThreshold());
        assertEquals(5, config.getSlidingWindowSize());
        assertEquals(3, config.getMinimumNumberOfCalls());
    }

    @Test
    @DisplayName("Circuit breaker metrics should be initialized")
    void shouldInitializeMetrics() {
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertEquals(-1.0f, metrics.getFailureRate(), 0.01f);
        assertEquals(0, metrics.getNumberOfBufferedCalls());
        assertEquals(0, metrics.getNumberOfFailedCalls());
        assertEquals(0, metrics.getNumberOfSuccessfulCalls());
    }

    @Test
    @DisplayName("Circuit breaker should track successful calls")
    void shouldTrackSuccessfulCalls() {
        for (int i = 0; i < 3; i++) {
            circuitBreaker.onSuccess(100, TimeUnit.NANOSECONDS);
        }

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertEquals(3, metrics.getNumberOfSuccessfulCalls());
        assertEquals(3, metrics.getNumberOfBufferedCalls());
        assertEquals(0.0f, metrics.getFailureRate(), 0.01f);
    }

    @Test
    @DisplayName("Circuit breaker should track failed calls")
    void shouldTrackFailedCalls() {
        for (int i = 0; i < 2; i++) {
            circuitBreaker.onSuccess(100, TimeUnit.NANOSECONDS);
        }
        circuitBreaker.onError(100, TimeUnit.NANOSECONDS, new RuntimeException("Test error"));

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertEquals(1, metrics.getNumberOfFailedCalls());
        assertEquals(2, metrics.getNumberOfSuccessfulCalls());
        assertEquals(3, metrics.getNumberOfBufferedCalls());
        assertEquals(33.33f, metrics.getFailureRate(), 0.5f);
    }

    @Test
    @DisplayName("Circuit breaker should not open below threshold")
    void shouldNotOpenBelowThreshold() {
        for (int i = 0; i < 3; i++) {
            circuitBreaker.onSuccess(100, TimeUnit.NANOSECONDS);
        }
        circuitBreaker.onError(100, TimeUnit.NANOSECONDS, new RuntimeException("Error"));

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    @DisplayName("Circuit breaker should transition from CLOSED to OPEN on threshold")
    void shouldTransitionToOpenOnThreshold() {
        circuitBreaker.onError(100, TimeUnit.NANOSECONDS, new RuntimeException("Error 1"));
        circuitBreaker.onError(100, TimeUnit.NANOSECONDS, new RuntimeException("Error 2"));
        circuitBreaker.onSuccess(100, TimeUnit.NANOSECONDS);

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    @DisplayName("Circuit breaker should transition from OPEN to HALF_OPEN after wait duration")
    void shouldTransitionToHalfOpenAfterWait() throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            circuitBreaker.onError(100, TimeUnit.NANOSECONDS, new RuntimeException("Error"));
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        Thread.sleep(1100);

        boolean permitted = circuitBreaker.tryAcquirePermission();

        assertTrue(permitted, "Should permit call after wait duration");
    }

    @Test
    @DisplayName("Circuit breaker should reset correctly")
    void shouldResetCorrectly() {
        for (int i = 0; i < 4; i++) {
            circuitBreaker.onError(100, TimeUnit.NANOSECONDS, new RuntimeException("Error"));
        }
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        circuitBreaker.reset();

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getMetrics().getNumberOfBufferedCalls());
    }
}
