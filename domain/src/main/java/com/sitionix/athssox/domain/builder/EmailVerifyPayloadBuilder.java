package com.sitionix.athssox.domain.builder;

import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;

import java.time.Instant;
import java.util.UUID;

/**
 * Builds email verification payloads used by outbox producer.
 */
public interface EmailVerifyPayloadBuilder {

    /**
     * Builds payload for notification delivery.
     *
     * @param userId      user id
     * @param siteId      site id
     * @param email       target email
     * @param traceId     trace id
     * @param requestedAt request time
     * @return payload ready for outbox enqueue
     */
    EmailVerifyPayload build(Long userId,
                             UUID siteId,
                             String email,
                             String traceId,
                             Instant requestedAt);
}
