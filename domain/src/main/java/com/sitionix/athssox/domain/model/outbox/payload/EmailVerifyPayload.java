package com.sitionix.athssox.domain.model.outbox.payload;

import com.sitionix.forge.outbox.core.port.EventMetadataContract;
import com.sitionix.forge.outbox.core.model.OutboxAggregateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;


@Data
@Builder
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class EmailVerifyPayload implements EventMetadataContract {

    private Delivery delivery;
    private NotificationTemplate template;
    private Params params;
    private Meta meta;

    @Override
    public UUID getIdempotencyId() {
        if (this.params != null && this.params.getEmailVerificationTokenId() != null) {
            return this.params.getEmailVerificationTokenId();
        }
        final String seed = String.join("|",
                this.getEventType(),
                this.meta == null || this.meta.getUserId() == null ? "" : String.valueOf(this.meta.getUserId()),
                this.meta == null || this.meta.getSiteId() == null ? "" : this.meta.getSiteId().toString(),
                this.meta == null || this.meta.getRequestedAt() == null ? "" : this.meta.getRequestedAt().toString(),
                this.delivery == null || this.delivery.getTo() == null ? "" : this.delivery.getTo());
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Instant getCreatedAt() {
        if (this.meta != null && this.meta.getRequestedAt() != null) {
            return this.meta.getRequestedAt();
        }
        return Instant.now();
    }

    @Override
    public String getEventType() {
        if (this.template != null) {
            return this.template.getDescription();
        }
        return NotificationTemplate.EMAIL_VERIFY.getDescription();
    }

    @Override
    public String getOutboxTraceId() {
        if (this.meta == null) {
            return null;
        }
        return this.meta.getTraceId();
    }

    @Override
    public OutboxAggregateType getAgregateType() {
        return OutboxAggregateType.USER;
    }

    @Override
    public Long getAgregateId() {
        if (this.meta == null) {
            return null;
        }
        return this.meta.getUserId();
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
