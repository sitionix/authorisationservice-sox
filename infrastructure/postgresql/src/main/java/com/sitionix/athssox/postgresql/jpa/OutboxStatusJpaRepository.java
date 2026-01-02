package com.sitionix.athssox.postgresql.jpa;

import com.sitionix.athssox.postgresql.entity.OutboxStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxStatusJpaRepository extends JpaRepository<OutboxStatusEntity, Long> {
}
