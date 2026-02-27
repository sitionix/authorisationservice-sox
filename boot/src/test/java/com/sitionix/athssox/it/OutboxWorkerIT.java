package com.sitionix.athssox.it;

import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.OutboxKafkaContracts;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate;
import com.sitionix.athssox.domain.model.outbox.payload.VerifyChannel;
import com.sitionix.forge.outbox.core.port.ForgeOutbox;
import com.sitionix.forge.outbox.core.port.ForgeOutboxWorker;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@IntegrationTest
class OutboxWorkerIT {

    @Autowired
    private ForgeOutboxWorker forgeOutboxWorker;

    @Autowired
    private TestManager testManager;

    @Autowired
    private ForgeOutbox forgeOutbox;

    @Test
    @DisplayName("given one outbox pattern when worker starts then publish verify event")
    void givenOutboxEventInDb_whenDispatchPendingEvents_thenPublishEvent() {
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
        this.forgeOutbox.send(payload);

        //when
        this.forgeOutboxWorker.dispatchPendingEvents();

        //then
        this.testManager.kafka()
                .consume(OutboxKafkaContracts.EMAIL_VERIFY_EVENT_ENVELOPE_KAFKA_CONTRACT)
                .assertPayload();
    }

    @Test
    @DisplayName("given sent email verify outbox event when worker starts then ignore event")
    void givenSentEmailVerifyOutboxEvent_whenDispatchPendingEvents_thenIgnoreEvent() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.FORGE_OUTBOX_EVENT_ENTITY_DB_CONTRACT.withJson("forgeOutboxEventSentOld.json"))
                .build();

        //when
        this.forgeOutboxWorker.dispatchPendingEvents();

        //then
        this.testManager.kafka()
                .consume(OutboxKafkaContracts.EMAIL_VERIFY_EVENT_ENVELOPE_KAFKA_CONTRACT)
                .await(Duration.ofSeconds(3))
                .assertNone();
    }
}
