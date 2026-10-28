package com.example.product.search.mapper;


import com.example.product.search.document.ProductDocument;
import com.example.product.search.dto.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public static ProductResponse toResponse(ProductDocument document) {
        if (document == null) {
            return null;
        }

        return new ProductResponse(
                document.getProductId(),
                document.getProductName(),
                document.getDescription(),
                document.getCategory(),
                document.getPrice(),
                document.getStockQuantity(),
                document.getSku(),
                document.getIsActive()
        );
    }
}