package com.globant.david.msglobantproducts.infrastructure.output;

import com.globant.david.msglobantproducts.domain.model.ProductDetail;
import com.globant.david.msglobantproducts.domain.repository.ProductRepository;
import com.globant.david.msglobantproducts.infrastructure.output.dto.ProductResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public class WebClientProductRepository implements ProductRepository {

    private static final Logger log = LoggerFactory.getLogger(WebClientProductRepository.class);
    private static final String SIMILAR_IDS_SERVICE = "productApiClient";
    private static final String PRODUCT_DETAIL_SERVICE = "productApiClient";

    private final ProductWebClient productWebClient;

    public WebClientProductRepository(ProductWebClient productWebClient) {
        this.productWebClient = productWebClient;
    }

    @Override
    @CircuitBreaker(name = SIMILAR_IDS_SERVICE, fallbackMethod = "getSimilarIdsFallback")
    @Retry(name = SIMILAR_IDS_SERVICE)
    @TimeLimiter(name = SIMILAR_IDS_SERVICE)
    public Mono<List<String>> findSimilarIds(String productId) {
        return productWebClient.getSimilarIds(productId)
                .doOnSuccess(ids -> log.debug("Found {} similar IDs for product {}", ids.size(), productId))
                .doOnError(e -> log.error("Error fetching similar IDs for product {}: {}", productId, e.getMessage()));
    }

    @Override
    @Cacheable(value = "product-details", key = "#productId")
    @CircuitBreaker(name = PRODUCT_DETAIL_SERVICE, fallbackMethod = "getProductDetailFallback")
    @Retry(name = PRODUCT_DETAIL_SERVICE)
    @TimeLimiter(name = PRODUCT_DETAIL_SERVICE)
    public Mono<ProductDetail> findProductDetail(String productId) {
        return productWebClient.getProduct(productId)
                .map(this::toProductDetail)
                .doOnSuccess(detail -> log.debug("Found product detail for {}", productId))
                .doOnError(e -> log.error("Error fetching product detail for {}: {}", productId, e.getMessage()));
    }

    @Override
    public Flux<ProductDetail> findProductDetails(List<String> productIds) {
        return Flux.fromIterable(productIds)
                .flatMap(this::findProductDetail);
    }

    private Mono<List<String>> getSimilarIdsFallback(String productId, Throwable throwable) {
        log.warn("Circuit breaker opened for findSimilarIds, returning empty list for product {}", productId);
        return Mono.just(List.of());
    }

    private Mono<ProductDetail> getProductDetailFallback(String productId, Throwable throwable) {
        log.warn("Circuit breaker opened for findProductDetail, returning empty for product {}", productId);
        return Mono.empty();
    }

    private ProductDetail toProductDetail(ProductResponse response) {
        return new ProductDetail(
                response.id(),
                response.name(),
                response.price(),
                response.availability()
        );
    }
}
