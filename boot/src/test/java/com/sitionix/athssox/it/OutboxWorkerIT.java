package com.sitionix.athssox.it;

import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.OutboxKafkaContracts;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.athssox.application.outbox.OutboxWorker;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

@IntegrationTest
class OutboxWorkerIT {

    @Autowired
    private OutboxWorker worker;

    @Autowired
    private TestManager testManager;

    @Test
    @DisplayName("given one outbox pattern when worker starts then publish verify event")
    void given_outbox_event_in_db_when_dispatch_pending_events_then_publish_event() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.OUTBOX_AGGREGATE_TYPE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_EVENT_TYPE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_INITIATOR_TYPE_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT.withJson("outboxEventEmailVerifyEntityPending.json"))
                .build();

        //when
        this.worker.dispatchPendingEvents();

        //then
        this.testManager.kafka()
                .consume(OutboxKafkaContracts.EMAIL_VERIFY_EVENT_ENVELOPE_KAFKA_CONTRACT)
                .assertPayload();
    }

    @Test
    @DisplayName("given non email verify outbox event when worker starts then ignore event")
    void given_non_email_verify_outbox_event_when_dispatch_pending_events_then_ignore_event() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.OUTBOX_AGGREGATE_TYPE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_EVENT_TYPE_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.OUTBOX_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_INITIATOR_TYPE_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT.withJson("outboxEventPasswordResetEntityPending.json"))
                .build();

        //when
        this.worker.dispatchPendingEvents();

        //then
        this.testManager.kafka()
                .consume(OutboxKafkaContracts.EMAIL_VERIFY_EVENT_ENVELOPE_KAFKA_CONTRACT)
                .await(Duration.ofSeconds(3))
                .assertNone();
    }
}
