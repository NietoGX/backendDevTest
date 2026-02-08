package com.globant.david.msglobantproducts.infrastructure.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.globant.david.msglobantproducts.domain.model.ProductDetail;
import com.globant.david.msglobantproducts.testsupport.IntegrationTest;
import com.globant.david.msglobantproducts.testsupport.ObjectMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cache Configuration")
class CacheConfigTest extends IntegrationTest {

    @Autowired
    private Cache<String, ProductDetail> productDetailCache;

    @Autowired
    private Cache<String, List<String>> similarIdsCache;

    @Test
    @DisplayName("Caches should be initialized correctly")
    void cachesShouldBeInitialized() {
        assertNotNull(productDetailCache);
        assertNotNull(similarIdsCache);
    }

    @Test
    @DisplayName("Should cache and retrieve product details")
    void shouldCacheAndRetrieveProductDetails() {
        String productId = ObjectMother.aProductId();
        ProductDetail product = ObjectMother.aProductDetail();

        productDetailCache.put(productId, product);

        ProductDetail cached = productDetailCache.getIfPresent(productId);
        assertNotNull(cached);
        assertEquals(product.id(), cached.id());
        assertEquals(product.name(), cached.name());
    }

    @Test
    @DisplayName("Should cache and retrieve similar IDs")
    void shouldCacheAndRetrieveSimilarIds() {
        String productId = ObjectMother.aProductId();
        List<String> similarIds = ObjectMother.aListOfProductIds();

        similarIdsCache.put(productId, similarIds);

        List<String> cached = similarIdsCache.getIfPresent(productId);
        assertNotNull(cached);
        assertEquals(3, cached.size());
        assertTrue(cached.contains("2"));
        assertTrue(cached.contains("3"));
        assertTrue(cached.contains("4"));
    }

    @Test
    @DisplayName("Should invalidate cache entries correctly")
    void shouldInvalidateCacheEntries() {
        String productId = ObjectMother.aProductId();
        ProductDetail product = ObjectMother.aProductDetail();
        productDetailCache.put(productId, product);

        productDetailCache.invalidate(productId);

        assertNull(productDetailCache.getIfPresent(productId));
    }

    @Test
    @DisplayName("Should clear all cache entries")
    void shouldClearAllCache() {
        ProductDetail product1 = ObjectMother.aProductDetail("1", "Product 1", new java.math.BigDecimal("10.00"));
        ProductDetail product2 = ObjectMother.aProductDetail("2", "Product 2", new java.math.BigDecimal("20.00"));
        productDetailCache.put("1", product1);
        productDetailCache.put("2", product2);
        similarIdsCache.put("1", ObjectMother.aListOfProductIds());

        productDetailCache.invalidateAll();
        similarIdsCache.invalidateAll();

        assertNull(productDetailCache.getIfPresent("1"));
        assertNull(productDetailCache.getIfPresent("2"));
        assertNull(similarIdsCache.getIfPresent("1"));
    }

    @Test
    @DisplayName("Should track cache size correctly")
    void shouldTrackCacheSize() {
        assertTrue(productDetailCache.estimatedSize() == 0);

        for (int i = 0; i < 100; i++) {
            ProductDetail product = ObjectMother.aProductDetail(
                    String.valueOf(i),
                    "Product " + i,
                    new java.math.BigDecimal("10.00")
            );
            productDetailCache.put(String.valueOf(i), product);
        }

        assertEquals(100, productDetailCache.estimatedSize());
    }

    @Test
    @DisplayName("Should handle null keys gracefully")
    void shouldHandleNullKeys() {
        assertNull(productDetailCache.getIfPresent("nonexistent"));
        assertNull(similarIdsCache.getIfPresent("nonexistent"));
    }

    @Test
    @DisplayName("Should overwrite existing cache entry")
    void shouldOverwriteExistingEntry() {
        String productId = ObjectMother.aProductId();
        ProductDetail original = ObjectMother.aProductDetail();
        ProductDetail updated = ObjectMother.anExpensiveProduct();

        productDetailCache.put(productId, original);

        productDetailCache.put(productId, updated);

        ProductDetail cached = productDetailCache.getIfPresent(productId);
        assertEquals(updated.id(), cached.id());
        assertEquals(updated.name(), cached.name());
        assertEquals(updated.price(), cached.price());
    }
}
