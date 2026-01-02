package com.sitionix.athssox.it;

import com.sitionix.athssox.domain.model.outbox.OutboxAggregateType;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate;
import com.sitionix.athssox.domain.model.outbox.payload.VerifyChannel;
import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class OutboxEventRepositoryIT {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private TestManager testManager;

    @Test
    void givenOutboxEventWithoutInitiator_whenCreate_thenPersistWithDefaultInitiator() {
        //given
        final UUID siteId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        final Instant requestedAt = Instant.parse("2025-12-23T18:31:16.740787Z");
        final OutboxEvent<EmailVerifyPayload> given = this.getOutboxEvent(siteId, requestedAt);

        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.OUTBOX_AGGREGATE_TYPE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_EVENT_TYPE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_INITIATOR_TYPE_ENTITY_DB_CONTRACT.getById(2L))
                .build();

        //when
        this.outboxEventRepository.create(given);

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "nextRetryAt", "payload", "createdAt", "updatedAt", "aggregateId")
                .containsWithJsonsStrict("outboxEventEmailVerifyEntity.json");
    }

    @Test
    void givenPendingOutboxEvent_whenClaimPendingEvents_thenMarkInProgressAndReturnEvent() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.OUTBOX_AGGREGATE_TYPE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_EVENT_TYPE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.OUTBOX_INITIATOR_TYPE_ENTITY_DB_CONTRACT.getById(2L))
                .build();

        final List<String> statuses = List.of(OutboxStatus.PENDING.getDescription(),
                OutboxStatus.FAILED.getDescription());
        final List<String> eventTypes = List.of(OutboxEventType.EMAIL_VERIFY.getDescription());
        final OutboxEvent<EmailVerifyPayload> pendingEvent = this.getOutboxEvent(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                Instant.parse("2025-12-23T18:31:16.740787Z"));

        this.outboxEventRepository.create(pendingEvent);

        //when
        final List<OutboxEvent<Object>> actual = this.outboxEventRepository.claimPendingEvents(statuses,
                eventTypes,
                10,
                LocalDateTime.now());

        //then
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getStatus()).isEqualTo(OutboxStatus.IN_PROGRESS);
        assertThat(actual.get(0).getEventType()).isEqualTo(OutboxEventType.EMAIL_VERIFY);
        assertThat(actual.get(0).getPayload()).isInstanceOf(EmailVerifyPayload.class);

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "createdAt", "updatedAt")
                .containsWithJsonsStrict("outboxEventEmailVerifyEntityInProgress.json");
    }

    private OutboxEvent<EmailVerifyPayload> getOutboxEvent(final UUID siteId,
                                                           final Instant requestedAt) {
        return OutboxEvent.<EmailVerifyPayload>builder()
                .aggregateType(OutboxAggregateType.USER)
                .aggregateId(1L)
                .eventType(OutboxEventType.EMAIL_VERIFY)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(LocalDateTime.parse("2024-01-01T00:00:00"))
                .payload(this.getEmailVerifyPayload(siteId, requestedAt))
                .lastError(null)
                .build();
    }

    private EmailVerifyPayload getEmailVerifyPayload(final UUID siteId,
                                                     final Instant requestedAt) {
        return EmailVerifyPayload.builder()
                .delivery(this.getDelivery())
                .template(NotificationTemplate.EMAIL_VERIFY)
                .params(this.getParams())
                .meta(this.getMeta(siteId, requestedAt))
                .build();
    }

    private EmailVerifyPayload.Delivery getDelivery() {
        return EmailVerifyPayload.Delivery.builder()
                .channel(VerifyChannel.EMAIL)
                .to("email@sitionix.com")
                .build();
    }

    private EmailVerifyPayload.Params getParams() {
        return EmailVerifyPayload.Params.builder()
                .verifyUrl("base-url/api/v1/auth/email/verify?token=YUScKCwVeN9CgzROymgdTJTT1agZvvWc798elW-kyBI&siteId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                .build();
    }

    private EmailVerifyPayload.Meta getMeta(final UUID siteId,
                                            final Instant requestedAt) {
        return EmailVerifyPayload.Meta.builder()
                .userId(1L)
                .siteId(siteId)
                .traceId(null)
                .requestedAt(requestedAt)
                .build();
    }
}
