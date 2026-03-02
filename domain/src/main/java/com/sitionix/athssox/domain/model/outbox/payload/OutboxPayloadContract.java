package com.sitionix.athssox.domain.model.outbox.payload;

import com.sitionix.forge.outbox.core.port.ForgeOutboxPayload;

/**
 * Contract for outbox payloads that expose event metadata fields.
 */
public interface OutboxPayloadContract extends ForgeOutboxPayload, EventMetadataContract {

    /**
     * @return outbox event type resolved from {@link #getEventType()}.
     */
    @Override
    default String getOutboxEventType() {
        return this.getEventType();
    }
}
