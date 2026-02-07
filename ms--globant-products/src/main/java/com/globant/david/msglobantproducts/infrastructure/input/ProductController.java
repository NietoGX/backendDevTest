package com.globant.david.msglobantproducts.infrastructure.input;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.globant.david.msglobantproducts.application.GetSimilarProductsUseCase;
import com.globant.david.msglobantproducts.domain.model.ProductDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final int MAX_CACHE_SIZE = 500;

    private final GetSimilarProductsUseCase getSimilarProductsUseCase;
    private final Cache<String, List<ProductDetail>> similarProductsCache;

    public ProductController(GetSimilarProductsUseCase getSimilarProductsUseCase) {
        this.getSimilarProductsUseCase = getSimilarProductsUseCase;
        this.similarProductsCache = Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterWrite(CACHE_TTL)
                .build();
    }

    @GetMapping("/{productId}/similar")
    public Flux<ProductDetail> getSimilarProducts(@PathVariable String productId) {
        return getCachedSimilarProducts(productId)
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<List<ProductDetail>> getCachedSimilarProducts(String productId) {
        List<ProductDetail> cached = similarProductsCache.getIfPresent(productId);
        if (cached != null) {
            log.debug("Cache hit for similar products of {}", productId);
            return Mono.just(cached);
        }

        log.info("Cache miss for similar products of {}", productId);
        return getSimilarProductsUseCase.execute(productId)
                .collectList()
                .doOnSuccess(result -> {
                    log.info("Retrieved {} products for {}", result.size(), productId);
                    similarProductsCache.put(productId, result);
                });
    }
}
