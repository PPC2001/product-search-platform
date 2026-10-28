package com.example.product.search.service;

import com.example.product.search.document.ProductDocument;
import com.example.product.search.event.ProductChangedEvent;
import com.example.product.search.repository.ProductSearchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProductSearchRepository productSearchRepository;

    @KafkaListener(topics = "product-events", groupId = "${spring.kafka.consumer.group-id:product-search-sync}")
    public void consume(String payload) throws Exception {
        ProductChangedEvent event = objectMapper.readValue(payload, ProductChangedEvent.class);

        log.info("Received product event: eventId={}, productId={}, eventType={}",
                event.eventId(), event.productId(), event.eventType());

        switch (event.eventType()) {
            case "PRODUCT_CREATED", "PRODUCT_UPDATED" -> upsertProduct(event);
            default -> log.warn("Unsupported product event type: {}", event.eventType());
        }
    }

    private void upsertProduct(ProductChangedEvent event) {
        ProductDocument document = ProductDocument.builder()
                .id(String.valueOf(event.productId()))
                .productId(event.productId())
                .productName(event.productName())
                .description(event.description())
                .category(event.category())
                .price(event.price())
                .stockQuantity(event.stockQuantity())
                .sku(event.sku())
                .isActive(event.active())
                .build();

        productSearchRepository.save(document);
        log.info("Elasticsearch index updated: productId={}", event.productId());
    }
}