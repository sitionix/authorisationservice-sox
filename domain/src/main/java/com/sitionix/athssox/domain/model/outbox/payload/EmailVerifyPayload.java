package com.sitionix.athssox.domain.model.outbox.payload;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;


@Getter
@Builder
@EqualsAndHashCode
public class EmailVerifyPayload {

    private Long id;
    private Delivery delivery;
    private NotificationTemplate template;
    private Params params;
    private Meta meta;

    @Getter
    @Builder
    @EqualsAndHashCode
    public static class Delivery {
        private VerifyChannel channel;
        private String to;
    }

    @Getter
    @Builder
    @EqualsAndHashCode
    public static class Params {
        private String verifyUrl;
    }

    @Getter
    @Builder
    @EqualsAndHashCode
    public static class Meta {
        private String user;
        private Long userId;
        private UUID siteId;
        private String traceId;
        private Instant requestedAt;
    }
}
