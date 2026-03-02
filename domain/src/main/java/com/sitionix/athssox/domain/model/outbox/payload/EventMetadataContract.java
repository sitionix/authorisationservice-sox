package com.sitionix.athssox.domain.model.outbox.payload;

import java.time.Instant;
import java.util.UUID;

/**
 * Contract for payload metadata used to build outbound event metadata.
 */
public interface EventMetadataContract {

    /**
     * @return idempotency identifier for the event.
     */
    UUID getIdempotencyId();

    /**
     * @return event creation timestamp.
     */
    Instant getCreatedAt();

    /**
     * @return domain event type.
     */
    String getEventType();
}
