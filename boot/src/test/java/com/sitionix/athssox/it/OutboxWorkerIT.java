package com.sitionix.athssox.it;

import com.sitionix.athssox.it.infra.OutboxKafkaContracts;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate;
import com.sitionix.athssox.domain.model.outbox.payload.VerifyChannel;
import com.sitionix.forge.outbox.core.port.ForgeOutbox;
import com.sitionix.forge.outbox.core.service.OutboxDispatcher;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@IntegrationTest
class OutboxWorkerIT {

    @Autowired
    private OutboxDispatcher outboxDispatcher;

    @Autowired
    private TestManager testManager;

    @Autowired
    private ForgeOutbox forgeOutbox;

    @Test
    @DisplayName("given one outbox pattern when worker starts then publish verify event")
    void given_outbox_event_in_db_when_dispatch_pending_events_then_publish_event() {
        //given
        final EmailVerifyPayload payload = EmailVerifyPayload.builder()
                .delivery(EmailVerifyPayload.Delivery.builder()
                        .channel(VerifyChannel.EMAIL)
                        .to("email@sitionix.com")
                        .build())
                .template(NotificationTemplate.EMAIL_VERIFY)
                .params(EmailVerifyPayload.Params.builder()
                        .emailVerificationTokenId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                        .pepperId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                        .build())
                .meta(EmailVerifyPayload.Meta.builder()
                        .userId(1L)
                        .siteId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                        .traceId(null)
                        .requestedAt(Instant.parse("2025-12-23T18:31:16.740787Z"))
                        .build())
                .build();
        this.forgeOutbox.enqueue(EmailVerifyPayload.OUTBOX_EVENT_TYPE, payload, Map.of(), Map.of(), "trace-it");

        //when
        this.outboxDispatcher.dispatchPendingEvents();

        //then
        this.testManager.kafka()
                .consume(OutboxKafkaContracts.EMAIL_VERIFY_EVENT_ENVELOPE_KAFKA_CONTRACT)
                .assertPayload();
    }

    @Test
    @DisplayName("given non email verify outbox event when worker starts then ignore event")
    void given_non_email_verify_outbox_event_when_dispatch_pending_events_then_ignore_event() {
        //given
        this.forgeOutbox.enqueue("PASSWORD_RESET", "{}", Map.of(), Map.of(), "trace-it-2");

        //when
        this.outboxDispatcher.dispatchPendingEvents();

        //then
        this.testManager.kafka()
                .consume(OutboxKafkaContracts.EMAIL_VERIFY_EVENT_ENVELOPE_KAFKA_CONTRACT)
                .await(Duration.ofSeconds(3))
                .assertNone();
    }
}
