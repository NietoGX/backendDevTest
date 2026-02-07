package com.globant.david.msglobantproducts.infrastructure.output.dto;

import java.math.BigDecimal;

public record ProductResponse(
        String id,
        String name,
        BigDecimal price,
        Boolean availability
) {}
