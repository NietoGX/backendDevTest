package com.globant.david.msglobantproducts.infrastructure.input;

import com.globant.david.msglobantproducts.application.GetSimilarProductsUseCase;
import com.globant.david.msglobantproducts.domain.model.ProductDetail;
import com.globant.david.msglobantproducts.testsupport.ObjectMother;
import com.globant.david.msglobantproducts.testsupport.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("ProductController")
class ProductControllerTest extends UnitTest {

    private WebTestClient webTestClient;
    private ProductController controller;

    @Mock
    private GetSimilarProductsUseCase getSimilarProductsUseCase;

    @BeforeEach
    void setUp() {
        controller = new ProductController(getSimilarProductsUseCase);
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    @DisplayName("Should return similar products successfully")
    void shouldReturnSimilarProducts() {
        List<ProductDetail> expectedProducts = ObjectMother.aListOfProductDetails();
        when(getSimilarProductsUseCase.execute(anyString()))
                .thenReturn(Flux.fromIterable(expectedProducts));

        webTestClient.get()
                .uri("/product/1/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductDetail.class)
                .hasSize(3)
                .contains(expectedProducts.toArray(new ProductDetail[0]));
    }

    @Test
    @DisplayName("Should return empty array when no similar products found")
    void shouldReturnEmptyWhenNoSimilarProducts() {
        when(getSimilarProductsUseCase.execute(anyString()))
                .thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/product/999/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductDetail.class)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should return products in correct order")
    void shouldReturnProductsInOrder() {
        List<ProductDetail> expectedProducts = List.of(
                ObjectMother.aProductDetail("1", "Shirt", new java.math.BigDecimal("10.00")),
                ObjectMother.aProductDetail("2", "Pants", new java.math.BigDecimal("20.00"))
        );
        when(getSimilarProductsUseCase.execute(anyString()))
                .thenReturn(Flux.fromIterable(expectedProducts));

        webTestClient.get()
                .uri("/product/1/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductDetail.class)
                .value(products -> {
                    assert products.get(0).id().equals("1");
                    assert products.get(1).id().equals("2");
                });
    }

    @Test
    @DisplayName("Should handle server error gracefully")
    void shouldHandleServerErrorGracefully() {
        when(getSimilarProductsUseCase.execute(anyString()))
                .thenReturn(Flux.error(new RuntimeException("Service unavailable")));

        webTestClient.get()
                .uri("/product/1/similar")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("Should return valid JSON structure")
    void shouldReturnValidJsonStructure() {
        ProductDetail product = ObjectMother.aProductDetail("1", "Product 1", new java.math.BigDecimal("10.00"));
        when(getSimilarProductsUseCase.execute(anyString()))
                .thenReturn(Flux.just(product));

        webTestClient.get()
                .uri("/product/1/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("1")
                .jsonPath("$[0].name").isEqualTo("Product 1")
                .jsonPath("$[0].price").isEqualTo(10.00)
                .jsonPath("$[0].availability").isEqualTo(true);
    }
}
