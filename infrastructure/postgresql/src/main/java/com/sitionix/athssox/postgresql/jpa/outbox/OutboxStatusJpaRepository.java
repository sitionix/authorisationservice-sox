package com.sitionix.athssox.postgresql.jpa.outbox;

import com.sitionix.athssox.postgresql.entity.outbox.OutboxStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxStatusJpaRepository extends JpaRepository<OutboxStatusEntity, Long> {
}
