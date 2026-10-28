package com.example.product.search.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductSearchRequest(
        @NotBlank(message = "Search query must not be blank")
        @Size(max = 100, message = "Search query must not exceed 100 characters")
        String q,

        @Min(value = 0, message = "Page must be zero or greater")
        int page,

        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size must not exceed 100")
        int size
) {
    public ProductSearchRequest {

    }
}