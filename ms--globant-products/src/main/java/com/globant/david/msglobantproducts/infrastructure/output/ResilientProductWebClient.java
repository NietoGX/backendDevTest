package com.globant.david.msglobantproducts.infrastructure.output;

import com.globant.david.msglobantproducts.infrastructure.output.dto.ProductResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class ResilientProductWebClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientProductWebClient.class);

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public ResilientProductWebClient(
            WebClient.Builder webClientBuilder,
            @Value("${external-api.base-url}") String baseUrl,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("productApiClient");
        this.retry = retryRegistry.retry("productApiClient");
    }

    public Mono<List<String>> getSimilarIds(String productId) {
        return webClient.get()
                .uri("/product/{id}/similarids", productId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .doOnError(e -> log.error("Error fetching similar IDs for product {}: {}", productId, e.getMessage()))
                .doOnSuccess(__ -> log.debug("Successfully fetched similar IDs for product {}", productId));
    }

    public Mono<ProductResponse> getProduct(String productId) {
        return webClient.get()
                .uri("/product/{id}", productId)
                .retrieve()
                .bodyToMono(ProductResponse.class)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .doOnError(e -> log.error("Error fetching product detail for {}: {}", productId, e.getMessage()))
                .doOnSuccess(__ -> log.debug("Successfully fetched product detail for {}", productId));
    }
}
