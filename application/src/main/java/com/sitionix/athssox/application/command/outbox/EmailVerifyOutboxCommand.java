package com.sitionix.athssox.application.command.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.athssox.domain.command.OutboxCommand;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.forge.outbox.core.model.OutboxEnqueueRequest;
import com.sitionix.forge.outbox.core.port.ForgeOutbox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailVerifyOutboxCommand implements OutboxCommand<EmailVerifyPayload> {

    private final ForgeOutbox forgeOutbox;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(final OutboxEvent<EmailVerifyPayload> outboxEvent) {
        this.forgeOutbox.enqueue(OutboxEnqueueRequest.builder()
                .eventType(outboxEvent.getEventType().getDescription())
                .payload(this.serializePayload(outboxEvent.getPayload()))
                .headers(Map.of())
                .metadata(this.metadata(outboxEvent))
                .traceId(this.traceId(outboxEvent))
                .aggregateType(outboxEvent.getAggregateType().getDescription())
                .aggregateId(outboxEvent.getAggregateId())
                .initiatorType(outboxEvent.getInitiatorType().getDescription())
                .initiatorId(outboxEvent.getInitiatorId())
                .nextAttemptAt(outboxEvent.getNextRetryAt().toInstant(ZoneOffset.UTC))
                .build());
    }

    private String serializePayload(final EmailVerifyPayload payload) {
        try {
            return this.objectMapper.writeValueAsString(payload);
        } catch (final JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize outbox payload", exception);
        }
    }

    private String traceId(final OutboxEvent<EmailVerifyPayload> outboxEvent) {
        if (outboxEvent.getPayload() == null || outboxEvent.getPayload().getMeta() == null) {
            return null;
        }
        return outboxEvent.getPayload().getMeta().getTraceId();
    }

    private Map<String, String> metadata(final OutboxEvent<EmailVerifyPayload> outboxEvent) {
        final Map<String, String> metadata = new HashMap<>();
        if (outboxEvent.getPayload() == null || outboxEvent.getPayload().getMeta() == null) {
            return Map.of();
        }
        final EmailVerifyPayload.Meta meta = outboxEvent.getPayload().getMeta();
        if (meta.getUserId() != null) {
            metadata.put("userId", String.valueOf(meta.getUserId()));
        }
        if (meta.getSiteId() != null) {
            metadata.put("siteId", meta.getSiteId().toString());
        }
        if (meta.getRequestedAt() != null) {
            metadata.put("requestedAt", meta.getRequestedAt().toString());
        }
        return metadata;
    }
}
