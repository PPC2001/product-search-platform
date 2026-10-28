package com.example.product.search.service;

import com.example.product.search.dto.ProductRequest;
import com.example.product.search.event.ProductChangedEvent;
import com.example.product.search.exception.ProductNotFoundException;
import com.example.product.search.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final JdbcTemplate jdbcTemplate;
    private final OutboxRepository outboxRepository;

    @Transactional
    public Long createProduct(ProductRequest request) {
        validate(request);

        long productId = generateProductId();

        try {
            jdbcTemplate.update("""
                            INSERT INTO PRATIK.PRODUCTS (
                                PRODUCT_ID, PRODUCT_NAME, DESCRIPTION, CATEGORY,
                                PRICE, STOCK_QUANTITY, SKU, IS_ACTIVE
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    productId,
                    request.productName().trim(),
                    request.description(),
                    request.category(),
                    request.price(),
                    request.stockQuantity() == null
                            ? 0 : request.stockQuantity(),
                    request.sku().trim(),
                    normalizeActive(request.active())
            );
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("Duplicate product ID or SKU.", exception);
        }

        ProductChangedEvent event = loadEvent(productId, "PRODUCT_CREATED");
        outboxRepository.save(event);
        return productId;
    }

    @Transactional
    public void updateProduct(Long productId, ProductRequest request) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Product ID must be positive.");
        }

        validate(request);

        int updatedRows = jdbcTemplate.update("""
                        UPDATE PRATIK.PRODUCTS
                        SET PRODUCT_NAME = ?,
                            DESCRIPTION = ?,
                            CATEGORY = ?,
                            PRICE = ?,
                            STOCK_QUANTITY = ?,
                            SKU = ?,
                            IS_ACTIVE = ?
                        WHERE PRODUCT_ID = ?
                        """,
                request.productName().trim(),
                request.description(),
                request.category(),
                request.price(),
                request.stockQuantity() == null
                        ? 0 : request.stockQuantity(),
                request.sku().trim(),
                normalizeActive(request.active()),
                productId
        );
        if (updatedRows == 0) {
            throw new ProductNotFoundException(productId);
        }
        try {
            outboxRepository.save(loadEvent(productId, "PRODUCT_UPDATED"));
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("The supplied SKU is already in use.", exception);
        }
    }

    private ProductChangedEvent loadEvent(
            Long productId, String eventType) {

        return jdbcTemplate.queryForObject("""
                        SELECT PRODUCT_ID, PRODUCT_NAME, DESCRIPTION, CATEGORY,
                               PRICE, STOCK_QUANTITY, SKU, IS_ACTIVE
                        FROM PRATIK.PRODUCTS
                        WHERE PRODUCT_ID = ?
                        """,
                (rs, rowNum) -> mapEvent(rs, eventType),
                productId
        );
    }

    private ProductChangedEvent mapEvent(ResultSet rs, String eventType) throws SQLException {
        return new ProductChangedEvent(
                UUID.randomUUID().toString(),
                rs.getLong("PRODUCT_ID"),
                eventType,
                rs.getString("PRODUCT_NAME"),
                rs.getString("DESCRIPTION"),
                rs.getString("CATEGORY"),
                rs.getBigDecimal("PRICE"),
                rs.getInt("STOCK_QUANTITY"),
                rs.getString("SKU"),
                "Y".equals(rs.getString("IS_ACTIVE")),
                Instant.now()
        );
    }

    private void validate(ProductRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Product request is required.");
        }

        if (request.productName() == null || request.productName().isBlank() || request.productName().length() > 150) {
            throw new IllegalArgumentException("Product name is required and must be at most 150 characters.");
        }

        if (request.sku() == null || request.sku().isBlank() || request.sku().length() > 50) {
            throw new IllegalArgumentException("SKU is required and must be at most 50 characters.");
        }

        if (request.description() != null && request.description().length() > 500) {
            throw new IllegalArgumentException("Description must be at most 500 characters.");
        }

        if (request.category() != null && request.category().length() > 100) {
            throw new IllegalArgumentException("Category must be at most 100 characters.");
        }

        if (request.price() == null || request.price().signum() < 0 || request.price().scale() > 2
                || request.price().compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException("Price must be between 0 and 99999999.99 with at most 2 decimal places.");
        }

        if (request.stockQuantity() != null && request.stockQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
        normalizeActive(request.active());
    }

    private String normalizeActive(String active) {
        String value = active == null || active.isBlank() ? "Y" : active.trim().toUpperCase();

        if (!value.equals("Y") && !value.equals("N")) {
            throw new IllegalArgumentException("Active must be Y or N.");
        }
        return value;
    }

    private Long generateProductId() {
        return jdbcTemplate.queryForObject(
                "SELECT PRODUCTS_SEQ.NEXTVAL FROM DUAL",
                Long.class
        );
    }
}