package com.sitionix.athssox.postgresql.jpa.outbox;

import com.sitionix.athssox.postgresql.entity.outbox.OutboxInitiatorTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxInitiatorTypeJpaRepository extends JpaRepository<OutboxInitiatorTypeEntity, Long> {
}
