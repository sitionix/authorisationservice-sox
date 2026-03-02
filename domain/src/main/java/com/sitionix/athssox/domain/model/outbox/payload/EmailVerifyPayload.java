package com.sitionix.athssox.domain.model.outbox.payload;

import com.sitionix.forge.outbox.core.port.ForgeOutboxPayload;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
        if (this.meta == null) {
            return Map.of();
        }
        final String userId = this.meta.getUserId() == null ? null : String.valueOf(this.meta.getUserId());
        final String siteId = this.meta.getSiteId() == null ? null : this.meta.getSiteId().toString();
        final String traceId = this.meta.getTraceId();
        final String requestedAt = this.meta.getRequestedAt() == null ? null : this.meta.getRequestedAt().toString();
        return Stream.of(
                        new AbstractMap.SimpleEntry<>("userId", userId),
                        new AbstractMap.SimpleEntry<>("siteId", siteId),
                        new AbstractMap.SimpleEntry<>("traceId", traceId),
                        new AbstractMap.SimpleEntry<>("requestedAt", requestedAt))
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
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
