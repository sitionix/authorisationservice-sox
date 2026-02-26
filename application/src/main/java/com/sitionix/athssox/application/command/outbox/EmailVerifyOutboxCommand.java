package com.sitionix.athssox.application.command.outbox;

import com.sitionix.athssox.domain.model.outbox.OutboxAggregateType;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.InitiatorType;
import com.sitionix.forge.outbox.core.command.AbstractForgeOutboxCommand;
import com.sitionix.forge.outbox.core.port.ForgeOutbox;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailVerifyOutboxCommand extends AbstractForgeOutboxCommand<EmailVerifyPayload> {

    public EmailVerifyOutboxCommand(final ForgeOutbox forgeOutbox) {
        super(forgeOutbox);
    }

    @Override
    protected String eventType() {
        return OutboxEventType.EMAIL_VERIFY.getDescription();
    }

    @Override
    protected String traceId(final EmailVerifyPayload payload) {
        if (payload == null || payload.getMeta() == null) {
            return null;
        }
        return payload.getMeta().getTraceId();
    }

    @Override
    protected Map<String, String> metadata(final EmailVerifyPayload payload) {
        final Map<String, String> metadata = new HashMap<>();
        if (payload == null || payload.getMeta() == null) {
            return Map.of();
        }
        final EmailVerifyPayload.Meta meta = payload.getMeta();
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

    @Override
    protected String aggregateType(final EmailVerifyPayload payload) {
        return OutboxAggregateType.USER.getDescription();
    }

    @Override
    protected Long aggregateId(final EmailVerifyPayload payload) {
        if (payload == null || payload.getMeta() == null) {
            return null;
        }
        return payload.getMeta().getUserId();
    }

    @Override
    protected String initiatorType(final EmailVerifyPayload payload) {
        return InitiatorType.USER.getDescription();
    }

    @Override
    protected String initiatorId(final EmailVerifyPayload payload) {
        final Long userId = this.aggregateId(payload);
        if (userId == null) {
            return null;
        }
        return String.valueOf(userId);
    }

    @Override
    protected Instant nextAttemptAt(final EmailVerifyPayload payload) {
        if (payload == null || payload.getMeta() == null) {
            return null;
        }
        return payload.getMeta().getRequestedAt();
    }
}
