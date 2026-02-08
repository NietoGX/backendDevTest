package com.globant.david.msglobantproducts.testsupport;

import com.globant.david.msglobantproducts.domain.model.ProductDetail;
import com.globant.david.msglobantproducts.infrastructure.output.dto.ProductResponse;

import java.math.BigDecimal;
import java.util.List;

public class ObjectMother {

    private ObjectMother() {
    }

    public static ProductDetail aProductDetail() {
        return new ProductDetail(
                "1",
                "Sample Product",
                new BigDecimal("19.99"),
                true
        );
    }

    public static ProductDetail aProductDetail(String id, String name, BigDecimal price) {
        return new ProductDetail(id, name, price, true);
    }

    public static ProductDetail anExpensiveProduct() {
        return new ProductDetail(
                "expensive-1",
                "Luxury Product",
                new BigDecimal("999.99"),
                true
        );
    }

    /**
     * Creates a ProductDetail with null id - useful for testing null filtering.
     * Note: This bypasses validation that would normally trim the id.
     */
    public static ProductDetail aProductDetailWithNullId() {
        return new ProductDetail(
                null,
                "Invalid Product",
                new BigDecimal("10.00"),
                true
        );
    }

    public static List<ProductDetail> aListOfProductDetails() {
        return List.of(
                aProductDetail("1", "Shirt", new BigDecimal("20.00")),
                aProductDetail("2", "Pants", new BigDecimal("30.00")),
                aProductDetail("3", "Shoes", new BigDecimal("50.00"))
        );
    }

    public static ProductResponse aProductResponse() {
        return new ProductResponse(
                "1",
                "Sample Product",
                new BigDecimal("19.99"),
                true
        );
    }

    public static ProductResponse aProductResponse(String id, String name, BigDecimal price) {
        return new ProductResponse(id, name, price, true);
    }

    /**
     * Creates a ProductResponse with null id - for testing null filtering.
     * The repository transforms this to a ProductDetail with null id.
     */
    public static ProductResponse aProductResponseWithNullId() {
        return new ProductResponse(null, "Invalid Product", new BigDecimal("10.00"), true);
    }

    public static List<ProductResponse> aListOfProductResponses() {
        return List.of(
                aProductResponse("1", "Shirt", new BigDecimal("20.00")),
                aProductResponse("2", "Pants", new BigDecimal("30.00")),
                aProductResponse("3", "Shoes", new BigDecimal("50.00"))
        );
    }

    public static String aProductId() {
        return "1";
    }

    public static List<String> aListOfProductIds() {
        return List.of("2", "3", "4");
    }

    public static List<String> aListOfProductIds(int count) {
        return List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
                .subList(0, Math.min(count, 10));
    }

    public static String anUnknownProductId() {
        return "999";
    }
}
