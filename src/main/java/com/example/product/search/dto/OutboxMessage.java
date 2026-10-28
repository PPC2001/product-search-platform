package com.example.product.search.dto;

public record OutboxMessage(
        long eventId,
        String eventUuid,
        String aggregateId,
        String eventType,
        String payload,
        int retryCount,
        String claimToken) {
}