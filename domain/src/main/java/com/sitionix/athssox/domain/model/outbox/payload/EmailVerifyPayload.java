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

    private Delivery delivery;
    private NotificationTemplate template;
    private Params params;
    private Meta meta;

    @Override
    public String getOutboxEventType() {
        return NotificationTemplate.EMAIL_VERIFY.getDescription();
    }

    @Override
    public Map<String, String> getOutboxMetadata() {
        return this.buildMetadata();
    }

    @Override
    public String getOutboxTraceId() {
        return this.meta == null ? null : this.meta.getTraceId();
    }

    @Override
    public String getOutboxAggregateType() {
        return this.meta == null ? null : "USER";
    }

    @Override
    public Long getOutboxAggregateId() {
        return this.meta == null ? null : this.meta.getUserId();
    }

    @Override
    public Instant getOutboxNextAttemptAt() {
        return this.meta == null ? null : this.meta.getRequestedAt();
    }

    private String metadataUserId() {
        if (this.meta == null || this.meta.getUserId() == null) {
            return null;
        }
        return String.valueOf(this.meta.getUserId());
    }

    private String metadataSiteId() {
        if (this.meta == null || this.meta.getSiteId() == null) {
            return null;
        }
        return this.meta.getSiteId().toString();
    }

    private String metadataTraceId() {
        if (this.meta == null) {
            return null;
        }
        return this.meta.getTraceId();
    }

    private String metadataRequestedAt() {
        if (this.meta == null || this.meta.getRequestedAt() == null) {
            return null;
        }
        return this.meta.getRequestedAt().toString();
    }

    private Map<String, String> buildMetadata() {
        final String userId = this.metadataUserId();
        final String siteId = this.metadataSiteId();
        final String traceId = this.metadataTraceId();
        final String requestedAt = this.metadataRequestedAt();
        if (userId == null && siteId == null && traceId == null && requestedAt == null) {
            return Map.of();
        }
        final Map<String, String> metadata = new HashMap<>();
        if (userId != null) {
            metadata.put("userId", userId);
        }
        if (siteId != null) {
            metadata.put("siteId", siteId);
        }
        if (traceId != null) {
            metadata.put("traceId", traceId);
        }
        if (requestedAt != null) {
            metadata.put("requestedAt", requestedAt);
        }
        return Map.copyOf(metadata);
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
