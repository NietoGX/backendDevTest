package com.globant.david.msglobantproducts.domain.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductDetail(

        @NotBlank(message = "Product ID is required")
        String id,

        @NotBlank(message = "Product name is required")
        String name,

        @NotNull(message = "Price is required")
        @Min(value = 0, message = "Price cannot be negative")
        BigDecimal price,

        @NotNull(message = "Availability is required")
        Boolean availability

) {
    public ProductDetail {
        id = id != null ? id.trim() : null;
        name = name != null ? name.trim() : null;
    }

    public static ProductDetail of(String id, String name, BigDecimal price) {
        return new ProductDetail(id, name, price, true);
    }
}
