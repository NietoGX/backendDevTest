package com.globant.david.msglobantproducts.infrastructure.output;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.globant.david.msglobantproducts.domain.model.ProductDetail;
import com.globant.david.msglobantproducts.domain.repository.ProductRepository;
import com.globant.david.msglobantproducts.infrastructure.output.dto.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Repository
public class WebClientProductRepository implements ProductRepository {

    private static final Logger log = LoggerFactory.getLogger(WebClientProductRepository.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final int MAX_CACHE_SIZE = 1000;

    private final ProductWebClient productWebClient;
    private final Cache<String, ProductDetail> productDetailCache;

    public WebClientProductRepository(ProductWebClient productWebClient) {
        this.productWebClient = productWebClient;
        this.productDetailCache = Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterWrite(CACHE_TTL)
                .build();
    }

    @Override
    public Mono<List<String>> findSimilarIds(String productId) {
        return productWebClient.getSimilarIds(productId)
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.just(List.of()))
                .onErrorResume(e -> {
                    log.error("Error fetching similar IDs: {}", e.getMessage());
                    return Mono.just(List.of());
                });
    }

    @Override
    public Mono<ProductDetail> findProductDetail(String productId) {
        ProductDetail cached = productDetailCache.getIfPresent(productId);
        if (cached != null) {
            log.debug("Cache hit for product detail: {}", productId);
            return Mono.just(cached);
        }

        log.debug("Cache miss for product detail: {}", productId);
        return productWebClient.getProduct(productId)
                .timeout(Duration.ofSeconds(3))
                .map(this::toProductDetail)
                .doOnNext(detail -> {
                    if (detail != null && detail.id() != null) {
                        productDetailCache.put(productId, detail);
                    }
                })
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.warn("Product not found: {}", productId);
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.error("Error fetching product detail for {}: {}", productId, e.getMessage());
                    return Mono.empty();
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
