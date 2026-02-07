package com.globant.david.msglobantproducts.application;

import com.globant.david.msglobantproducts.domain.model.ProductDetail;
import com.globant.david.msglobantproducts.domain.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
                .doOnNext(ids -> log.debug("Found {} similar IDs for product {}", ids.size(), productId))
                .flatMapMany(ids -> productRepository.findProductDetails(ids))
                .doOnComplete(() -> log.debug("Completed fetching similar products for {}", productId));
    }
}
