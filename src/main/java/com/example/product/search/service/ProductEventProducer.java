package com.example.product.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductEventProducer {

    private static final String TOPIC = "product-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publishTestEvent(String message) {
        kafkaTemplate.send(TOPIC, message)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to publish Kafka event", exception);
                        return;
                    }

                    log.info(
                            "Published event to topic={}, partition={}, offset={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }
}