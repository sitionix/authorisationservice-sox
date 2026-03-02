package com.sitionix.athssox.domain.model.outbox.payload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sitionix.forge.outbox.core.port.ForgeOutboxPayload;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.LinkedHashMap;
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

    @JsonIgnore
    private Outbox outbox;

    @Override
    public String getOutboxEventType() {
        return OutboxEventType.EMAIL_VERIFY.getDescription();
    }

    @Override
    public Map<String, String> getOutboxMetadata() {
        if (this.outbox == null || this.outbox.getMetadata() == null) {
            return Map.of();
        }
        return this.outbox.getMetadata().toMap();
    }

    @Override
    public String getOutboxTraceId() {
        return this.outbox == null ? null : this.outbox.getTraceId();
    }

    @Override
    public String getOutboxAggregateType() {
        return this.outbox == null ? null : this.outbox.getAggregateType();
    }

    @Override
    public Long getOutboxAggregateId() {
        return this.outbox == null ? null : this.outbox.getAggregateId();
    }

    @Override
    public Instant getOutboxNextAttemptAt() {
        return this.outbox == null ? null : this.outbox.getNextAttemptAt();
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

    @Data
    @Builder
    @Jacksonized
    @EqualsAndHashCode
    public static class Outbox {
        private OutboxMetadata metadata;
        private String traceId;
        private String aggregateType;
        private Long aggregateId;
        private Instant nextAttemptAt;
    }

    @Data
    @Builder
    @Jacksonized
    @EqualsAndHashCode
    public static class OutboxMetadata {
        private Long userId;
        private UUID siteId;
        private String traceId;
        private Instant requestedAt;

        public Map<String, String> toMap() {
            final Map<String, String> metadata = new LinkedHashMap<>();
            this.putIfPresent(metadata, OutboxMetadataKey.USER_ID, this.userId == null ? null : String.valueOf(this.userId));
            this.putIfPresent(metadata, OutboxMetadataKey.SITE_ID, this.siteId == null ? null : this.siteId.toString());
            this.putIfPresent(metadata, OutboxMetadataKey.TRACE_ID, this.traceId);
            this.putIfPresent(metadata,
                    OutboxMetadataKey.REQUESTED_AT,
                    this.requestedAt == null ? null : this.requestedAt.toString());
            return metadata.isEmpty() ? Map.of() : Map.copyOf(metadata);
        }

        private void putIfPresent(final Map<String, String> metadata,
                                  final OutboxMetadataKey key,
                                  final String value) {
            if (value != null) {
                metadata.put(key.getKey(), value);
            }
        }
    }

    private enum OutboxMetadataKey {
        USER_ID("userId"),
        SITE_ID("siteId"),
        TRACE_ID("traceId"),
        REQUESTED_AT("requestedAt");

        private final String key;

        OutboxMetadataKey(final String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }
    }
}
