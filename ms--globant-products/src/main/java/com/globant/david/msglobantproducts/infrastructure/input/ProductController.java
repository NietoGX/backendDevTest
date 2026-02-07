package com.globant.david.msglobantproducts.infrastructure.input;

import com.globant.david.msglobantproducts.application.GetSimilarProductsUseCase;
import com.globant.david.msglobantproducts.domain.model.ProductDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/product")
public class ProductController {

    private final GetSimilarProductsUseCase getSimilarProductsUseCase;

    public ProductController(GetSimilarProductsUseCase getSimilarProductsUseCase) {
        this.getSimilarProductsUseCase = getSimilarProductsUseCase;
    }

    @GetMapping("/{productId}/similar")
    public Flux<ProductDetail> getSimilarProducts(@PathVariable String productId) {
        return getSimilarProductsUseCase.execute(productId);
    }
}
