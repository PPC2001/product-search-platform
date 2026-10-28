package com.example.product.search.event;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductChangedEvent(
        String eventId,
        Long productId,
        String eventType,
        String productName,
        String description,
        String category,
        BigDecimal price,
        Integer stockQuantity,
        String sku,
        Boolean active,
        Instant occurredAt
) {}
