package com.example.product.search.outbox;

import com.example.product.search.dto.OutboxMessage;
import com.example.product.search.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${outbox.publisher.batch-size:5}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:1000}")
    public void publishPendingEvents() {
        List<OutboxMessage> messages = outboxRepository.claimBatch(batchSize);

        if (messages.isEmpty()) {
            return;
        }
        log.info("Claimed {} outbox events", messages.size());
        for (OutboxMessage message : messages) {
            publishMessage(message);
        }
    }

    private void publishMessage(OutboxMessage message) {
        try {
            // Use the event UUID as Kafka key for consistent partitioning.
            kafkaTemplate.send("product-events", message.eventUuid(), message.payload()).get(15, TimeUnit.SECONDS);

            // Mark published only after Kafka acknowledges the message.
            outboxRepository.markPublished(message);

            log.info("Published outbox event. eventId={}, eventUuid={}, eventType={}", message.eventId(),
                    message.eventUuid(), message.eventType());

        } catch (Exception exception) {
            log.error("Failed to publish outbox event. eventId={}, eventUuid={}", message.eventId(),
                    message.eventUuid(), exception);

            try {
                outboxRepository.recordFailure(message, exception);
            } catch (Exception updateException) {
                log.error("Failed to record outbox failure. eventId={}, " + "eventUuid={}. The claim lease will allow recovery.",
                        message.eventId(), message.eventUuid(), updateException
                );
            }
        }
    }
}
