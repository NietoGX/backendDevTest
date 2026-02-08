package com.globant.david.msglobantproducts.infrastructure.output;

import com.github.benmanes.caffeine.cache.Cache;
import com.globant.david.msglobantproducts.domain.model.ProductDetail;
import com.globant.david.msglobantproducts.infrastructure.output.dto.ProductResponse;
import com.globant.david.msglobantproducts.testsupport.ObjectMother;
import com.globant.david.msglobantproducts.testsupport.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("WebClientProductRepository")
class WebClientProductRepositoryTest extends UnitTest {

    @Mock
    private ResilientProductWebClient productWebClient;

    @Mock
    private Cache<String, ProductDetail> productDetailCache;

    @Mock
    private Cache<String, List<String>> similarIdsCache;

    private WebClientProductRepository repository;

    @BeforeEach
    void setUp() {
        repository = new WebClientProductRepository(
                productWebClient,
                productDetailCache,
                similarIdsCache
        );
    }

    @Test
    @DisplayName("Should return cached similar IDs when available")
    void shouldReturnCachedSimilarIds() {
        String productId = ObjectMother.aProductId();
        List<String> cachedIds = ObjectMother.aListOfProductIds();
        when(similarIdsCache.getIfPresent(productId)).thenReturn(cachedIds);

        Mono<List<String>> result = repository.findSimilarIds(productId);

        StepVerifier.create(result)
                .expectNext(cachedIds)
                .verifyComplete();

        verify(productWebClient, never()).getSimilarIds(anyString());
    }

    @Test
    @DisplayName("Should fetch and cache similar IDs when not cached")
    void shouldFetchAndCacheSimilarIds() {
        String productId = ObjectMother.aProductId();
        List<String> ids = ObjectMother.aListOfProductIds();
        when(similarIdsCache.getIfPresent(productId)).thenReturn(null);
        when(productWebClient.getSimilarIds(productId)).thenReturn(Mono.just(ids));

        Mono<List<String>> result = repository.findSimilarIds(productId);

        StepVerifier.create(result)
                .expectNext(ids)
                .verifyComplete();

        verify(similarIdsCache).put(productId, ids);
    }

    @Test
    @DisplayName("Should return empty list when similar IDs not found (404)")
    void shouldReturnEmptyListWhenNotFound() {
        String productId = ObjectMother.anUnknownProductId();
        when(similarIdsCache.getIfPresent(productId)).thenReturn(null);
        WebClientResponseException notFound = WebClientResponseException.create(
                404, "Not Found", null, null, StandardCharsets.UTF_8
        );
        when(productWebClient.getSimilarIds(productId)).thenReturn(Mono.error(notFound));

        Mono<List<String>> result = repository.findSimilarIds(productId);

        StepVerifier.create(result)
                .expectNext(List.of())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty list on generic error")
    void shouldReturnEmptyListOnError() {
        String productId = ObjectMother.aProductId();
        when(similarIdsCache.getIfPresent(productId)).thenReturn(null);
        when(productWebClient.getSimilarIds(anyString())).thenReturn(Mono.error(new RuntimeException("Service error")));

        Mono<List<String>> result = repository.findSimilarIds(productId);

        StepVerifier.create(result)
                .expectNext(List.of())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return cached product detail when available")
    void shouldReturnCachedProductDetail() {
        String productId = ObjectMother.aProductId();
        ProductDetail cached = ObjectMother.aProductDetail();
        when(productDetailCache.getIfPresent(productId)).thenReturn(cached);

        Mono<ProductDetail> result = repository.findProductDetail(productId);

        StepVerifier.create(result)
                .expectNext(cached)
                .verifyComplete();

        verify(productWebClient, never()).getProduct(anyString());
    }

    @Test
    @DisplayName("Should fetch and cache product detail when not cached")
    void shouldFetchAndCacheProductDetail() {
        String productId = ObjectMother.aProductId();
        ProductResponse response = ObjectMother.aProductResponse();
        when(productDetailCache.getIfPresent(productId)).thenReturn(null);
        when(productWebClient.getProduct(anyString())).thenReturn(Mono.just(response));

        Mono<ProductDetail> result = repository.findProductDetail(productId);

        StepVerifier.create(result)
                .expectNextMatches(p ->
                        p.id().equals(response.id()) &&
                                p.name().equals(response.name()) &&
                                p.price().equals(response.price()) &&
                                p.availability() == response.availability())
                .verifyComplete();

        verify(productDetailCache).put(eq(productId), any(ProductDetail.class));
    }

    @Test
    @DisplayName("Should return empty when product not found (404)")
    void shouldReturnEmptyWhenProductNotFound() {
        String productId = ObjectMother.anUnknownProductId();
        when(productDetailCache.getIfPresent(productId)).thenReturn(null);
        WebClientResponseException notFound = WebClientResponseException.create(
                404, "Not Found", null, null, StandardCharsets.UTF_8
        );
        when(productWebClient.getProduct(productId)).thenReturn(Mono.error(notFound));

        Mono<ProductDetail> result = repository.findProductDetail(productId);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty on generic error")
    void shouldReturnEmptyOnError() {
        String productId = ObjectMother.aProductId();
        when(productDetailCache.getIfPresent(productId)).thenReturn(null);
        when(productWebClient.getProduct(anyString())).thenReturn(Mono.error(new RuntimeException("Service error")));

        Mono<ProductDetail> result = repository.findProductDetail(productId);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fetch multiple product details concurrently (order not guaranteed)")
    void shouldFetchMultipleProductDetails() {
        List<String> productIds = ObjectMother.aListOfProductIds(3);
        List<ProductResponse> responses = ObjectMother.aListOfProductResponses();

        when(productDetailCache.getIfPresent(anyString())).thenReturn(null);
        for (int i = 0; i < responses.size(); i++) {
            when(productWebClient.getProduct(productIds.get(i)))
                    .thenReturn(Mono.just(responses.get(i)));
        }

        Flux<ProductDetail> result = repository.findProductDetails(productIds);

        StepVerifier.create(result.collectList())
                .expectNextMatches(list ->
                        list.size() == 3 &&
                                list.stream().map(ProductDetail::id).toList()
                                        .containsAll(List.of("1", "2", "3")))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should filter out products with null id in batch fetch")
    void shouldFilterNullProductsInBatchFetch() {
        List<String> productIds = List.of("1", "2");
        ProductResponse response1 = ObjectMother.aProductResponse();
        ProductResponse responseWithNullId = ObjectMother.aProductResponseWithNullId();

        when(productDetailCache.getIfPresent(anyString())).thenReturn(null);
        when(productWebClient.getProduct("1"))
                .thenReturn(Mono.just(response1));
        when(productWebClient.getProduct("2"))
                .thenReturn(Mono.just(responseWithNullId));

        Flux<ProductDetail> result = repository.findProductDetails(productIds);

        StepVerifier.create(result)
                .expectNextMatches(p -> p.id().equals("1"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should filter out products with null id when mixed with valid products")
    void shouldFilterNullProductsWhenMixedWithValid() {
        List<String> productIds = List.of("1", "2", "3", "4");
        List<ProductResponse> responses = List.of(
                ObjectMother.aProductResponse("1", "Product 1", new java.math.BigDecimal("10.00")),
                ObjectMother.aProductResponse("2", "Product 2", new java.math.BigDecimal("20.00")),
                ObjectMother.aProductResponseWithNullId(),
                ObjectMother.aProductResponse("4", "Product 4", new java.math.BigDecimal("40.00"))
        );

        when(productDetailCache.getIfPresent(anyString())).thenReturn(null);
        for (int i = 0; i < productIds.size(); i++) {
            when(productWebClient.getProduct(productIds.get(i)))
                    .thenReturn(Mono.just(responses.get(i)));
        }

        Flux<ProductDetail> result = repository.findProductDetails(productIds);

        StepVerifier.create(result.collectList())
                .expectNextMatches(list ->
                        list.size() == 3 &&
                                list.stream().map(ProductDetail::id).toList()
                                        .containsAll(List.of("1", "2", "4")) &&
                                list.stream().noneMatch(p -> p.id() == null))
                .verifyComplete();
    }
}
