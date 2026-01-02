package com.sitionix.athssox.domain.model.outbox.payload;

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
public class EmailVerifyPayload {

    private Delivery delivery;
    private NotificationTemplate template;
    private Params params;
    private Meta meta;

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
        private String verifyUrl;
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
