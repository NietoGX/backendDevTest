package com.globant.david.msglobantproducts.infrastructure.output;

import com.github.benmanes.caffeine.cache.Cache;
import com.globant.david.msglobantproducts.domain.model.ProductDetail;
import com.globant.david.msglobantproducts.domain.repository.ProductRepository;
import com.globant.david.msglobantproducts.infrastructure.output.dto.ProductResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public class WebClientProductRepository implements ProductRepository {

    private static final Logger log = LoggerFactory.getLogger(WebClientProductRepository.class);

    private final ResilientProductWebClient productWebClient;
    private final Cache<String, ProductDetail> productDetailCache;
    private final Cache<String, List<String>> similarIdsCache;

    public WebClientProductRepository(
            ResilientProductWebClient productWebClient,
            Cache<String, ProductDetail> productDetailCache,
            Cache<String, List<String>> similarIdsCache) {
        this.productWebClient = productWebClient;
        this.productDetailCache = productDetailCache;
        this.similarIdsCache = similarIdsCache;
    }

    @Override
    public Mono<List<String>> findSimilarIds(String productId) {
        List<String> cached = similarIdsCache.getIfPresent(productId);
        if (cached != null) {
            return Mono.just(cached);
        }
        return productWebClient.getSimilarIds(productId)
                .onErrorResume(CallNotPermittedException.class, e -> {
                    log.warn("Circuit breaker is OPEN for similar IDs - serving cached or empty response");
                    return Mono.just(List.of());
                })
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.just(List.of()))
                .onErrorResume(e -> {
                    log.error("Error fetching similar IDs: {}", e.getMessage());
                    return Mono.just(List.of());
                })
                .doOnNext(ids -> similarIdsCache.put(productId, ids));
    }

    @Override
    public Mono<ProductDetail> findProductDetail(String productId) {
        ProductDetail cached = productDetailCache.getIfPresent(productId);
        if (cached != null) {
            return Mono.just(cached);
        }
        return productWebClient.getProduct(productId)
                .map(this::toProductDetail)
                .onErrorResume(CallNotPermittedException.class, e -> {
                    log.warn("Circuit breaker is OPEN for product detail - serving cached or empty response");
                    return Mono.empty();
                })
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.warn("Product not found: {}", productId);
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.error("Error fetching product detail for {}: {}", productId, e.getMessage());
                    return Mono.empty();
                })
                .doOnNext(detail -> {
                    productDetailCache.put(productId, detail);
                });
    }

    @Override
    public Flux<ProductDetail> findProductDetails(List<String> productIds) {
        return Flux.fromIterable(productIds)
                .flatMap(this::findProductDetail, 10)
                .filter(detail -> detail.id() != null);
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
