package com.sitionix.athssox.domain.model.outbox.payload;

import com.sitionix.forge.outbox.core.port.ForgeOutboxPayload;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Data
@Builder
@Jacksonized
@EqualsAndHashCode
public class EmailVerifyPayload implements ForgeOutboxPayload {

    public static final String OUTBOX_EVENT_TYPE = "EMAIL_VERIFY";

    private Delivery delivery;
    private NotificationTemplate template;
    private Params params;
    private Meta meta;

    @Override
    public String getOutboxEventType() {
        return OUTBOX_EVENT_TYPE;
    }

    @Override
    public Map<String, String> getOutboxMetadata() {
        if (this.meta == null) {
            return Map.of();
        }
        final Map<String, String> metadata = new HashMap<>();
        if (this.meta.getUserId() != null) {
            metadata.put("userId", String.valueOf(this.meta.getUserId()));
        }
        if (this.meta.getSiteId() != null) {
            metadata.put("siteId", this.meta.getSiteId().toString());
        }
        if (this.meta.getRequestedAt() != null) {
            metadata.put("requestedAt", this.meta.getRequestedAt().toString());
        }
        return Map.copyOf(metadata);
    }

    @Override
    public String getOutboxTraceId() {
        return this.meta == null ? null : this.meta.getTraceId();
    }

    @Override
    public String getOutboxAggregateType() {
        return "USER";
    }

    @Override
    public Long getOutboxAggregateId() {
        return this.meta == null ? null : this.meta.getUserId();
    }

    @Override
    public Instant getOutboxNextAttemptAt() {
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
