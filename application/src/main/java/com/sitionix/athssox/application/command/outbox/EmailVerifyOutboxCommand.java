package com.sitionix.athssox.application.command.outbox;

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

    @Override
    public void execute(final OutboxEvent<EmailVerifyPayload> outboxEvent) {
        this.forgeOutbox.enqueue(OutboxEnqueueRequest.builder()
                .eventType(outboxEvent.getEventType().getDescription())
                .payloadObject(outboxEvent.getPayload())
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
