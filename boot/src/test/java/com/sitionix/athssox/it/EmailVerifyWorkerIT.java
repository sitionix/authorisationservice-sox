package com.sitionix.athssox.it;

import com.sitionix.athssox.it.infra.OutboxKafkaContracts;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.worker.EmailVerifyOutboxWorker;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

@IntegrationTest
class EmailVerifyWorkerIT {

    @Autowired
    private EmailVerifyOutboxWorker worker;

    @Autowired
    private TestManager testManager;

    @Test
    @DisplayName("given one outbox pattern when worker start then publish and verify event")
    void givenOutboxEventInDB_whenDispatchPendingEmailVerifyEvents_thenPublishEvent() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.OUTBOX_AGGREGATE_TYPE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_EVENT_TYPE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT.withJson("outboxEventEmailVerifyEntityPending.json"))
                .build();

        //when
        this.worker.dispatchPendingEmailVerifyEvents();

        //then
        this.testManager.kafka()
                .consume(OutboxKafkaContracts.EMAIL_VERIFY_EVENT_ENVELOPE_KAFKA_CONTRACT)
                .ignoreFields("verifyUrl", "traceId")
                .assertPayload();
    }

    @Test
    @DisplayName("given non email verify outbox event when worker starts then ignore event")
    void givenNonEmailVerifyOutboxEvent_whenDispatchPendingEmailVerifyEvents_thenIgnoreEvent() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.OUTBOX_AGGREGATE_TYPE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_EVENT_TYPE_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.OUTBOX_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT.withJson("outboxEventPasswordResetEntityPending.json"))
                .build();

        //when
        this.worker.dispatchPendingEmailVerifyEvents();

        //then
        this.testManager.kafka()
                .consume(OutboxKafkaContracts.EMAIL_VERIFY_EVENT_ENVELOPE_KAFKA_CONTRACT)
                .await(Duration.ofSeconds(3))
                .assertNone();
    }
}
