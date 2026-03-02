package com.sitionix.athssox.domain.model.outbox.payload;

import java.time.Instant;
import java.util.UUID;

public interface EventMetadataContract {

    UUID getIdempotencyId();

    Instant getCreatedAt();

    String getEventType();
}
