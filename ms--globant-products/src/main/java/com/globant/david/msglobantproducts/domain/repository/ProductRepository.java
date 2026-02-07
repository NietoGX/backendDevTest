package com.globant.david.msglobantproducts.domain.repository;

import com.globant.david.msglobantproducts.domain.model.ProductDetail;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ProductRepository {

    Mono<List<String>> findSimilarIds(String productId);

    Mono<ProductDetail> findProductDetail(String productId);

    Flux<ProductDetail> findProductDetails(List<String> productIds);
}
