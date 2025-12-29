package com.sitionix.athssox.postgresql.repository;

import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import com.sitionix.athssox.postgresql.entity.OutboxEventEntity;
import com.sitionix.athssox.postgresql.jpa.OutboxEventJpaRepository;
import com.sitionix.athssox.postgresql.mapper.outbox.OutboxInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    private final OutboxInfraMapper outboxInfraMapper;

    @Override
    public void create(final OutboxEvent<?> outboxEventCreate) {
        final OutboxEventEntity entity = this.outboxInfraMapper.toEntity(outboxEventCreate);
        this.outboxEventJpaRepository.save(entity);
    }
}
