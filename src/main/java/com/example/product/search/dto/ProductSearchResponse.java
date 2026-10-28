package com.example.product.search.dto;


import java.util.List;

public record ProductSearchResponse(
        List<ProductResponse> products,
        long totalElements,
        int page,
        int size,
        int totalPages
) {
}
