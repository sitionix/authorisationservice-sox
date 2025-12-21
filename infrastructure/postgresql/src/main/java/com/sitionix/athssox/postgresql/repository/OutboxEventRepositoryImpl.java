package com.sitionix.athssox.postgresql.repository;

import com.sitionix.athssox.domain.model.outbox.OutboxEventCreate;
import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import com.sitionix.athssox.postgresql.entity.OutboxAggregateTypeEntity;
import com.sitionix.athssox.postgresql.entity.OutboxEventEntity;
import com.sitionix.athssox.postgresql.entity.OutboxEventTypeEntity;
import com.sitionix.athssox.postgresql.entity.OutboxStatusEntity;
import com.sitionix.athssox.postgresql.jpa.OutboxEventJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final EntityManager entityManager;

    @Override
    public void create(final OutboxEventCreate outboxEventCreate) {
        final OutboxEventEntity outboxEvent = new OutboxEventEntity();
        outboxEvent.setAggregateType(this.entityManager.getReference(OutboxAggregateTypeEntity.class,
                outboxEventCreate.getAggregateType().getId()));
        outboxEvent.setAggregateId(outboxEventCreate.getAggregateId());
        outboxEvent.setEventType(this.entityManager.getReference(OutboxEventTypeEntity.class,
                outboxEventCreate.getEventType().getId()));
        outboxEvent.setStatus(this.entityManager.getReference(OutboxStatusEntity.class,
                outboxEventCreate.getStatus().getId()));
        outboxEvent.setRetryCount(outboxEventCreate.getRetryCount());
        outboxEvent.setNextRetryAt(outboxEventCreate.getNextRetryAt());
        outboxEvent.setPayload(outboxEventCreate.getPayload());
        outboxEvent.setLastError(outboxEventCreate.getLastError());

        this.outboxEventJpaRepository.save(outboxEvent);
    }
}
