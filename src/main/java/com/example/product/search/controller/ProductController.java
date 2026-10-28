package com.example.product.search.controller;

import com.example.product.search.dto.MessageResponse;
import com.example.product.search.dto.ProductCreatedResponse;
import com.example.product.search.dto.ProductRequest;
import com.example.product.search.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Products", description = "Create and update products (Oracle write side)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Create a product",
            description = "Writes the product and an outbox event in one Oracle transaction.")
    @PostMapping
    public ResponseEntity<ProductCreatedResponse> createProduct(@RequestBody ProductRequest request) {
        Long productId = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ProductCreatedResponse("Product created successfully", productId)
        );
    }

    @Operation(summary = "Update a product",
            description = "Updates the product and records a PRODUCT_UPDATED outbox event atomically.")
    @PutMapping("/{productId}")
    public ResponseEntity<MessageResponse> updateProduct(@PathVariable Long productId,
                                                         @RequestBody ProductRequest request) {
        productService.updateProduct(productId, request);
        return ResponseEntity.ok(new MessageResponse("Product updated successfully"));
    }
}