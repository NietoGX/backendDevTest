package com.globant.david.msglobantproducts.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.globant.david.msglobantproducts.domain.model.ProductDetail;
import com.globant.david.msglobantproducts.domain.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class GetSimilarProductsUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetSimilarProductsUseCase.class);

    private final ProductRepository productRepository;

    public GetSimilarProductsUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Flux<ProductDetail> execute(String productId) {
        return productRepository.findSimilarIds(productId)
                .flatMapMany(productRepository::findProductDetails);
    }
}
