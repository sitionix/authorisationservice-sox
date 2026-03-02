package com.sitionix.athssox.pipe.producer;

import com.sitionix.athssox.domain.model.outbox.payload.EventMetadataContract;
import com.sitionix.forge.outbox.core.port.ForgeOutboxPublishMetadata;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class OutboxEventMetadata implements EventMetadataContract {

    private final UUID idempotencyId;
    private final Instant createdAt;
    private final String eventType;

    private OutboxEventMetadata(final UUID idempotencyId,
                                final Instant createdAt,
                                final String eventType) {
        this.idempotencyId = idempotencyId;
        this.createdAt = createdAt;
        this.eventType = eventType;
    }

    public static OutboxEventMetadata from(final ForgeOutboxPublishMetadata metadata) {
        final UUID idempotencyId = Objects.requireNonNull(metadata.getIdempotencyId(), "idempotencyId is required");
        final Instant createdAt = Objects.requireNonNull(metadata.getCreatedAt(), "createdAt is required");
        final String eventType = Objects.requireNonNull(metadata.getEventType(), "eventType is required");
        if (eventType.isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        return new OutboxEventMetadata(idempotencyId, createdAt, eventType);
    }

    @Override
    public UUID getIdempotencyId() {
        return this.idempotencyId;
    }

    @Override
    public Instant getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public String getEventType() {
        return this.eventType;
    }
}
