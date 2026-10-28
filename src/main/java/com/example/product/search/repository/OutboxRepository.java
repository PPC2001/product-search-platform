package com.example.product.search.repository;

import com.example.product.search.dto.OutboxMessage;
import com.example.product.search.event.ProductChangedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void save(ProductChangedEvent event) {
        final String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize product event", exception);
        }

        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO OUTBOX_EVENTS (
                    EVENT_ID, EVENT_UUID, AGGREGATE_TYPE, AGGREGATE_ID,
                    EVENT_TYPE, PAYLOAD, EVENT_STATUS, RETRY_COUNT, CREATED_AT
                ) VALUES (
                    OUTBOX_EVENTS_SEQ.NEXTVAL, ?, 'PRODUCT', ?, ?, ?,
                    'NEW', 0, SYSTIMESTAMP
                )
                """);

            statement.setString(1, event.eventId());
            statement.setString(2, event.productId().toString());
            statement.setString(3, event.eventType());
            statement.setString(4, payload);
            return statement;
        });
    }

    private static final RowMapper<OutboxMessage> MESSAGE_MAPPER =
            (rs, rowNum) -> new OutboxMessage(
                    rs.getLong("EVENT_ID"),
                    rs.getString("EVENT_UUID"),
                    rs.getString("AGGREGATE_ID"),
                    rs.getString("EVENT_TYPE"),
                    rs.getString("PAYLOAD"),
                    rs.getInt("RETRY_COUNT"),
                    null
            );

    /**
     * Claims a small batch in a short Oracle transaction.
     * Expired PROCESSING records can be reclaimed after a crash.
     */
    @Transactional
    public List<OutboxMessage> claimBatch(int batchSize) {
        List<OutboxMessage> selected = jdbcTemplate.query("""
                SELECT EVENT_ID, EVENT_UUID, AGGREGATE_ID,
                       EVENT_TYPE, PAYLOAD, RETRY_COUNT
                FROM OUTBOX_EVENTS
                WHERE (
                    EVENT_STATUS = 'NEW'
                    AND (NEXT_ATTEMPT_AT IS NULL
                         OR NEXT_ATTEMPT_AT <= SYSTIMESTAMP)
                ) OR (
                    EVENT_STATUS = 'PROCESSING'
                    AND LOCKED_UNTIL < SYSTIMESTAMP
                )
                AND ROWNUM <= ?
                FOR UPDATE SKIP LOCKED
                """, MESSAGE_MAPPER, batchSize);

        List<OutboxMessage> claimed = new ArrayList<>();

        for (OutboxMessage message : selected) {
            String token = UUID.randomUUID().toString();

            jdbcTemplate.update("""
                    UPDATE OUTBOX_EVENTS
                    SET EVENT_STATUS = 'PROCESSING',
                        CLAIM_TOKEN = ?,
                        LOCKED_UNTIL =
                            SYSTIMESTAMP + NUMTODSINTERVAL(60, 'SECOND')
                    WHERE EVENT_ID = ?
                    """, token, message.eventId());

            claimed.add(new OutboxMessage(
                    message.eventId(),
                    message.eventUuid(),
                    message.aggregateId(),
                    message.eventType(),
                    message.payload(),
                    message.retryCount(),
                    token
            ));
        }

        return claimed;
    }

    @Transactional
    public void markPublished(OutboxMessage message) {
        int updated = jdbcTemplate.update("""
                UPDATE OUTBOX_EVENTS
                SET EVENT_STATUS = 'PUBLISHED',
                    PUBLISHED_AT = SYSTIMESTAMP,
                    CLAIM_TOKEN = NULL,
                    LOCKED_UNTIL = NULL,
                    LAST_ERROR = NULL
                WHERE EVENT_ID = ?
                  AND EVENT_STATUS = 'PROCESSING'
                  AND CLAIM_TOKEN = ?
                """, message.eventId(), message.claimToken());

        if (updated != 1) {
            throw new IllegalStateException(
                    "Outbox claim expired or was replaced for event "
                            + message.eventUuid());
        }
    }

    @Transactional
    public void recordFailure(OutboxMessage message, Throwable failure) {
        String error = failure.getMessage();

        if (error == null || error.isBlank()) {
            error = failure.getClass().getSimpleName();
        }

        if (error.length() > 2000) {
            error = error.substring(0, 2000);
        }

        jdbcTemplate.update("""
                UPDATE OUTBOX_EVENTS
                SET RETRY_COUNT = RETRY_COUNT + 1,
                    EVENT_STATUS = CASE
                        WHEN RETRY_COUNT + 1 >= 10 THEN 'FAILED'
                        ELSE 'NEW'
                    END,
                    NEXT_ATTEMPT_AT = CASE
                        WHEN RETRY_COUNT + 1 >= 10 THEN NULL
                        ELSE SYSTIMESTAMP
                             + NUMTODSINTERVAL(
                                 LEAST(POWER(2, RETRY_COUNT), 300),
                                 'SECOND'
                             )
                    END,
                    LAST_ERROR = ?,
                    CLAIM_TOKEN = NULL,
                    LOCKED_UNTIL = NULL
                WHERE EVENT_ID = ?
                  AND EVENT_STATUS = 'PROCESSING'
                  AND CLAIM_TOKEN = ?
                """, error, message.eventId(), message.claimToken());
    }
}