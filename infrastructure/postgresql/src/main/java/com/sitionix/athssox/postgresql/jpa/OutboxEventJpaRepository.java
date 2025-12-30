package com.sitionix.athssox.postgresql.jpa;

import com.sitionix.athssox.postgresql.entity.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e from OutboxEventEntity e
            where e.status.description in :statuses
              and e.eventType.description = :eventType
              and e.nextRetryAt <= :now
            order by e.nextRetryAt asc
            """)
    List<OutboxEventEntity> findPendingForUpdate(@Param("statuses") List<String> statuses,
                                                 @Param("eventType") String eventType,
                                                 @Param("now") LocalDateTime now,
                                                 Pageable pageable);
}
