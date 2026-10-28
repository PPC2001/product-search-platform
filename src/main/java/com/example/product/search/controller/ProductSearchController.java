package com.example.product.search.controller;


import com.example.product.search.dto.ProductResponse;
import com.example.product.search.dto.ProductSearchResponse;
import com.example.product.search.service.ProductSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Tag(name = "Product Search", description = "Elasticsearch-backed product search")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService searchService;

    @Operation(summary = "Search products",
            description = "Fuzzy full-text search on product name with category/price filters, sorting and paging.")
    @GetMapping("/search")
    public ProductSearchResponse search(
            @Parameter(description = "Search text (1-100 chars)", example = "iphone")
            @RequestParam("q") String query,
            @Parameter(description = "Exact category filter", example = "Mobiles")
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @Parameter(description = "relevance or price")
            @RequestParam(defaultValue = "relevance") String sortBy,
            @Parameter(description = "asc or desc (used when sortBy=price)")
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, 1-100")
            @RequestParam(defaultValue = "10") int size) {
        return searchService.search(query, category, minPrice, maxPrice, activeOnly, sortBy, sortDirection, page, size);
    }

    @Operation(summary = "Fuzzy search by product name")
    @GetMapping("/fuzzy")
    public List<ProductResponse> searchByProductNameFuzzy(@RequestParam("q") String query) {
        return searchService.searchByProductNameFuzzy(query);
    }
}
