package com.sitionix.athssox.domain.builder;

import com.sitionix.athssox.domain.model.emailverify.EmailVerifyPayloadContext;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;

/**
 * Builds email verification payloads used by outbox producer.
 */
public interface EmailVerifyPayloadBuilder {

    /**
     * Builds payload for notification delivery.
     *
     * @param context source context for payload construction
     * @return payload ready for outbox enqueue
     */
    EmailVerifyPayload build(EmailVerifyPayloadContext context);
}
