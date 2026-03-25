package com.sitionix.athssox.domain.model.outbox;

import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.forge.outbox.core.model.ForgeOutboxEventType;
import com.sitionix.forge.outbox.core.port.ForgeOutboxPayload;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum NotificationOutboxEventType implements ForgeOutboxEventType {
    EMAIL_VERIFY(1L, "EMAIL_VERIFY", EmailVerifyPayload.class);

    @Getter
    private final Long id;

    @Getter
    private final String description;

    private final Class<? extends ForgeOutboxPayload> payloadClass;

    @Override
    public Class<? extends ForgeOutboxPayload> payloadClass() {
        return this.payloadClass;
    }
}
