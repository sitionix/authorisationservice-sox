package com.sitionix.athssox.it;

import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.OutboxKafkaContracts;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.forge.outbox.core.port.ForgeOutboxWorker;
import com.sitionix.forge.outbox.testkit.postgres.contract.ForgeOutboxPostgresDbContracts;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

@IntegrationTest
class OutboxWorkerIT {

    @Autowired
    private ForgeOutboxWorker forgeOutboxWorker;

    @Autowired
    private TestManager testManager;

    @Test
    @DisplayName("given one outbox pattern when worker starts then publish verify event")
    void givenOutboxEventInDb_whenDispatchPendingEvents_thenPublishEvent() {
        //given
        this.testManager.postgresql()
                .create()
                .to(ForgeOutboxPostgresDbContracts.FORGE_OUTBOX_EVENT_ENTITY_DB_CONTRACT
                        .withJson("forgeOutboxEventPendingEmailVerify.json"))
                .build();

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
                .to(ForgeOutboxPostgresDbContracts.FORGE_OUTBOX_EVENT_ENTITY_DB_CONTRACT.withJson("forgeOutboxEventSentOld.json"))
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
