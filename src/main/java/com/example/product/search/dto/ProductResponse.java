package com.example.product.search.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long productId,
        String productName,
        String description,
        String category,
        BigDecimal price,
        Integer stockQuantity,
        String sku,
        Boolean active
) {
}