package com.sitionix.athssox.domain.model.outbox.payload;

import com.sitionix.forge.outbox.core.port.ForgeOutboxPayload;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;


@Data
@Builder
@Jacksonized
@EqualsAndHashCode
public class EmailVerifyPayload implements ForgeOutboxPayload {

    private Delivery delivery;
    private NotificationTemplate template;
    private Params params;
    private Meta meta;

    @Override
    public String getOutboxEventType() {
        return NotificationTemplate.EMAIL_VERIFY.getDescription();
    }

    @Override
    public Long userId() {
        return this.meta == null ? null : this.meta.getUserId();
    }

    @Override
    public UUID siteId() {
        return this.meta == null ? null : this.meta.getSiteId();
    }

    @Override
    public String traceId() {
        return this.meta == null ? null : this.meta.getTraceId();
    }

    @Override
    public Instant requestedAt() {
        return this.meta == null ? null : this.meta.getRequestedAt();
    }

    @Data
    @Builder
    @Jacksonized
    @EqualsAndHashCode
    public static class Delivery {
        private VerifyChannel channel;

        @ToString.Exclude
        private String to;
    }

    @Data
    @Builder
    @Jacksonized
    @EqualsAndHashCode
    public static class Params {
        private UUID emailVerificationTokenId;
        private UUID pepperId;
    }

    @Data
    @Builder
    @Jacksonized
    @EqualsAndHashCode
    public static class Meta {
        private Long userId;
        private UUID siteId;
        private String traceId;
        private Instant requestedAt;
    }
}
