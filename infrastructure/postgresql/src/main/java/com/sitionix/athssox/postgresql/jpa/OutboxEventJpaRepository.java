package com.sitionix.athssox.postgresql.jpa;

import com.sitionix.athssox.postgresql.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {
}
