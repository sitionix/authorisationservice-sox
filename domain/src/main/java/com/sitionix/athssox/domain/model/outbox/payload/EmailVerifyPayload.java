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
    public Long metadataUserId() {
        if (this.meta == null || this.meta.getUserId() == null) {
            return null;
        }
        return this.meta.getUserId();
    }

    @Override
    public UUID metadataSiteId() {
        if (this.meta == null || this.meta.getSiteId() == null) {
            return null;
        }
        return this.meta.getSiteId();
    }

    @Override
    public String metadataTraceId() {
        if (this.meta == null) {
            return null;
        }
        return this.meta.getTraceId();
    }

    @Override
    public Instant metadataRequestedAt() {
        if (this.meta == null || this.meta.getRequestedAt() == null) {
            return null;
        }
        return this.meta.getRequestedAt();
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
