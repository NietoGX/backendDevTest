package com.globant.david.msglobantproducts.integration;

import com.globant.david.msglobantproducts.domain.model.ProductDetail;
import com.globant.david.msglobantproducts.testsupport.WireMockIntegrationTest;
import com.globant.david.msglobantproducts.testsupport.WireMockStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product API Integration Tests")
class ProductIntegrationTests extends WireMockIntegrationTest {

    private WireMockStubs stubs;

    @BeforeEach
    void setUpStubs() {
        stubs = new WireMockStubs(wireMockServer);
    }

    @Nested
    @DisplayName("Product API End-to-End Tests")
    class ProductApiE2ETests {

        @Test
        @DisplayName("Should return similar products successfully")
        void shouldReturnSimilarProducts() {
            stubs.stubSimilarIds("1", "2", "3", "4");
            stubs.stubProduct("2", "Dress Shirt", 19.99, true);
            stubs.stubProduct("3", "M Jeans", 29.99, true);
            stubs.stubProduct("4", "Blue Socks", 9.99, true);

            List<ProductDetail> result = webTestClient.get()
                    .uri(productSimilarUrl("1"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals("Content-Type", "application/json")
                    .expectBodyList(ProductDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertEquals(3, result.size());
            assertTrue(result.stream().anyMatch(p -> p.id().equals("2")));
            assertTrue(result.stream().anyMatch(p -> p.id().equals("3")));
            assertTrue(result.stream().anyMatch(p -> p.id().equals("4")));

            stubs.verifySimilarIdsCalled("1");
            stubs.verifyProductCalled("2");
            stubs.verifyProductCalled("3");
            stubs.verifyProductCalled("4");
        }

        @Test
        @DisplayName("Should return empty array when product has no similar products")
        void shouldReturnEmptyWhenNoSimilarProducts() {
            stubs.stubSimilarIds("999");

            List<ProductDetail> result = webTestClient.get()
                    .uri(productSimilarUrl("999"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(ProductDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertTrue(result.isEmpty());
            stubs.verifySimilarIdsCalled("999");
        }

        @Test
        @DisplayName("Should return empty array when similar IDs endpoint returns 404")
        void shouldReturnEmptyWhenSimilarIdsNotFound() {
            stubs.stubSimilarIdsNotFound("999");

            List<ProductDetail> result = webTestClient.get()
                    .uri(productSimilarUrl("999"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(ProductDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertTrue(result.isEmpty());
            stubs.verifySimilarIdsCalled("999");
        }

        @Test
        @DisplayName("Should filter out products that return 404")
        void shouldFilterOutProductsNotFound() {
            stubs.stubSimilarIds("1", "2", "3", "4");
            stubs.stubProduct("2", "Dress Shirt", 19.99, true);
            stubs.stubProductNotFound("3");
            stubs.stubProduct("4", "Blue Socks", 9.99, true);

            List<ProductDetail> result = webTestClient.get()
                    .uri(productSimilarUrl("1"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(ProductDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.stream().anyMatch(p -> p.id().equals("2")));
            assertTrue(result.stream().anyMatch(p -> p.id().equals("4")));
            assertFalse(result.stream().anyMatch(p -> p.id().equals("3")));
        }

        @Test
        @DisplayName("Should return JSON with correct structure")
        void shouldReturnCorrectJsonStructure() {
            stubs.stubSimilarIds("1", "2");
            stubs.stubProduct("2", "Classic Shirt", 39.99, true);

            webTestClient.get()
                    .uri(productSimilarUrl("1"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$[0].id").isEqualTo("2")
                    .jsonPath("$[0].name").isEqualTo("Classic Shirt")
                    .jsonPath("$[0].price").isEqualTo(39.99)
                    .jsonPath("$[0].availability").isEqualTo(true);
        }

        @Test
        @DisplayName("Should handle service degradation gracefully")
        void shouldHandleServiceDownGracefully() {
            stubs.stubSimilarIdsError("1", 503);

            List<ProductDetail> result = webTestClient.get()
                    .uri(productSimilarUrl("1"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(ProductDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle concurrent requests")
        void shouldHandleConcurrentRequests() {
            stubs.stubSimilarIds("1", "2", "3", "4", "5");
            stubs.stubProduct("2", "Product 2", 20.00, true);
            stubs.stubProduct("3", "Product 3", 30.00, true);
            stubs.stubProduct("4", "Product 4", 40.00, true);
            stubs.stubProduct("5", "Product 5", 50.00, true);

            Mono<List<ProductDetail>> request1 = fetchSimilarProducts("1");
            Mono<List<ProductDetail>> request2 = fetchSimilarProducts("1");
            Mono<List<ProductDetail>> request3 = fetchSimilarProducts("1");

            List<ProductDetail> results = Mono.zip(request1, request2, request3)
                    .flatMap(tuple -> {
                        List<ProductDetail> combined = new java.util.ArrayList<>();
                        combined.addAll(tuple.getT1());
                        combined.addAll(tuple.getT2());
                        combined.addAll(tuple.getT3());
                        return Mono.just(combined);
                    })
                    .block();

            assertNotNull(results);
            assertTrue(results.size() >= 3);
        }

        private Mono<List<ProductDetail>> fetchSimilarProducts(String productId) {
            return webTestClient.get()
                    .uri(productSimilarUrl(productId))
                    .exchange()
                    .expectStatus().isOk()
                    .returnResult(ProductDetail.class)
                    .getResponseBody()
                    .collectList();
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Tests")
    class CircuitBreakerTests {

        @Test
        @DisplayName("Should handle service degradation gracefully")
        void shouldHandleServiceDegradationGracefully() {
            stubs.stubSimilarIdsError("1", 503);

            List<?> result = webTestClient.get()
                    .uri(productSimilarUrl("1"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(Object.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty array when service is down")
        void shouldReturnEmptyWhenServiceDown() {
            stubs.stubSimilarIdsError("1", 503);

            List<ProductDetail> result = webTestClient.get()
                    .uri(productSimilarUrl("1"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(ProductDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should track circuit breaker metrics")
        void shouldTrackCircuitBreakerMetrics() {
            stubs.stubSimilarIdsError("1", 503);

            webTestClient.get()
                    .uri(productSimilarUrl("1"))
                    .exchange()
                    .expectStatus().isOk();

            String metrics = webTestClient.get()
                    .uri(circuitBreakersUrl())
                    .exchange()
                    .expectStatus().isOk()
                    .returnResult(String.class)
                    .getResponseBody()
                    .blockFirst();

            assertNotNull(metrics);
            assertTrue(metrics.contains("productApiClient"));
        }
    }

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return healthy status")
        void shouldReturnHealthyStatus() {
            webTestClient.get()
                    .uri(healthUrl())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("UP")
                    .jsonPath("$.components.diskSpace.status").isEqualTo("UP")
                    .jsonPath("$.components.ping.status").isEqualTo("UP")
                    .jsonPath("$.components.livenessState.status").isEqualTo("UP")
                    .jsonPath("$.components.readinessState.status").isEqualTo("UP");
        }

        @Test
        @DisplayName("Should return circuit breakers status")
        void shouldReturnCircuitBreakersStatus() {
            webTestClient.get()
                    .uri(circuitBreakersUrl())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.circuitBreakers.productApiClient.state").exists()
                    .jsonPath("$.circuitBreakers.productApiClient.state").isEqualTo("CLOSED");
        }

        @Test
        @DisplayName("Should track circuit breaker metrics after request")
        void shouldTrackCircuitBreakerMetricsAfterRequest() {
            stubs.stubSimilarIds("1", "2");
            stubs.stubProduct("2", "Test Product", 10.00, true);

            webTestClient.get()
                    .uri(productSimilarUrl("1"))
                    .exchange()
                    .expectStatus().isOk();

            webTestClient.get()
                    .uri(circuitBreakersUrl())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.circuitBreakers.productApiClient").exists();
        }
    }
}
