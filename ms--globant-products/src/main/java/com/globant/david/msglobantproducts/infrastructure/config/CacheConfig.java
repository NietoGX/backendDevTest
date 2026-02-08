package com.globant.david.msglobantproducts.infrastructure.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.globant.david.msglobantproducts.domain.model.ProductDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.util.List;

@Configuration
public class CacheConfig {

    @Bean
    @Profile("!integration")
    public Cache<String, ProductDetail> productDetailCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();
    }

    @Bean
    @Profile("!integration")
    public Cache<String, List<ProductDetail>> similarProductsCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
    }

    @Bean
    @Profile("!integration")
    public Cache<String, List<String>> similarIdsCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
    }

    // No-op cache beans for integration tests
    @Bean
    @Profile("integration")
    public Cache<String, ProductDetail> productDetailCacheNoOp() {
        return Caffeine.newBuilder().maximumSize(0).build();
    }

    @Bean
    @Profile("integration")
    public Cache<String, List<ProductDetail>> similarProductsCacheNoOp() {
        return Caffeine.newBuilder().maximumSize(0).build();
    }

    @Bean
    @Profile("integration")
    public Cache<String, List<String>> similarIdsCacheNoOp() {
        return Caffeine.newBuilder().maximumSize(0).build();
    }
}
