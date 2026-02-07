package com.globant.david.msglobantproducts.infrastructure.input;

import com.globant.david.msglobantproducts.application.GetSimilarProductsUseCase;
import com.globant.david.msglobantproducts.domain.model.ProductDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/product")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final GetSimilarProductsUseCase getSimilarProductsUseCase;

    public ProductController(GetSimilarProductsUseCase getSimilarProductsUseCase) {
        this.getSimilarProductsUseCase = getSimilarProductsUseCase;
    }

    @GetMapping("/{productId}/similar")
    public Flux<ProductDetail> getSimilarProducts(@PathVariable String productId) {
        log.info("Received request for similar products of {}", productId);
        return getSimilarProductsUseCase.execute(productId)
                .doOnComplete(() -> log.info("Successfully retrieved similar products for {}", productId))
                .doOnError(e -> log.error("Error retrieving similar products for {}: {}", productId, e.getMessage()));
    }
}
