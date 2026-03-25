package com.sitionix.athssox.domain.model.outbox.payload;

import com.sitionix.athssox.domain.model.outbox.NotificationOutboxEventType;
import com.sitionix.forge.outbox.core.model.OutboxAggregateType;
import com.sitionix.forge.outbox.core.port.ForgeOutboxPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;


@Data
@Builder
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class EmailVerifyPayload implements ForgeOutboxPayload {

    private Delivery delivery;
    private NotificationTemplate template;
    private Params params;
    private Meta meta;

    @Override
    public String eventType() {
        return NotificationOutboxEventType.EMAIL_VERIFY.getDescription();
    }

    @Override
    public OutboxAggregateType aggregateType() {
        return OutboxAggregateType.USER;
    }

    @Override
    public Long aggregateId() {
        if (this.meta == null) {
            return null;
        }
        return this.meta.getUserId();
    }

    @Override
    public String traceId() {
        if (this.meta == null) {
            return null;
        }
        return this.meta.getTraceId();
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
