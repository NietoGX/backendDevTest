package com.globant.david.msglobantproducts.application;

import com.globant.david.msglobantproducts.domain.model.ProductDetail;
import com.globant.david.msglobantproducts.domain.repository.ProductRepository;
import com.globant.david.msglobantproducts.testsupport.ObjectMother;
import com.globant.david.msglobantproducts.testsupport.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("GetSimilarProductsUseCase")
class GetSimilarProductsUseCaseTest extends UnitTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private GetSimilarProductsUseCase useCase;

    @Test
    @DisplayName("Should return similar products when repository returns data")
    void shouldReturnSimilarProducts() {
        String productId = ObjectMother.aProductId();
        List<String> similarIds = ObjectMother.aListOfProductIds();
        List<ProductDetail> expectedProducts = ObjectMother.aListOfProductDetails();

        when(productRepository.findSimilarIds(productId))
                .thenReturn(Mono.just(similarIds));
        when(productRepository.findProductDetails(eq(similarIds)))
                .thenReturn(Flux.fromIterable(expectedProducts));

        Flux<ProductDetail> result = useCase.execute(productId);

        StepVerifier.create(result)
                .expectNextSequence(expectedProducts)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty flux when no similar products found")
    void shouldReturnEmptyWhenNoSimilarProducts() {
        String productId = ObjectMother.anUnknownProductId();

        when(productRepository.findSimilarIds(productId))
                .thenReturn(Mono.just(List.of()));
        when(productRepository.findProductDetails(eq(List.of())))
                .thenReturn(Flux.empty());

        Flux<ProductDetail> result = useCase.execute(productId);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return products in order")
    void shouldReturnProductsInOrder() {
        String productId = ObjectMother.aProductId();
        List<String> similarIds = ObjectMother.aListOfProductIds();
        List<ProductDetail> expectedProducts = ObjectMother.aListOfProductDetails();

        when(productRepository.findSimilarIds(productId))
                .thenReturn(Mono.just(similarIds));
        when(productRepository.findProductDetails(eq(similarIds)))
                .thenReturn(Flux.fromIterable(expectedProducts));

        Flux<ProductDetail> result = useCase.execute(productId);

        StepVerifier.create(result)
                .expectNextSequence(expectedProducts)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should propagate error from repository")
    void shouldPropagateError() {
        String productId = ObjectMother.aProductId();
        RuntimeException exception = new RuntimeException("Service unavailable");

        when(productRepository.findSimilarIds(productId))
                .thenReturn(Mono.error(exception));

        Flux<ProductDetail> result = useCase.execute(productId);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Service unavailable"))
                .verify();
    }
}
