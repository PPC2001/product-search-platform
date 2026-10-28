package com.example.product.search.dto;

import java.math.BigDecimal;

public record ProductRequest(
        String productName,
        String description,
        String category,
        BigDecimal price,
        Integer stockQuantity,
        String sku,
        String active
) {}