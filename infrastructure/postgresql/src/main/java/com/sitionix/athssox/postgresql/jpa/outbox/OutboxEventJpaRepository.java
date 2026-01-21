package com.sitionix.athssox.postgresql.jpa.outbox;

import com.sitionix.athssox.postgresql.entity.outbox.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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
              and e.eventType.description in :eventTypes
              and e.nextRetryAt <= :now
            order by e.nextRetryAt asc
            """)
    List<OutboxEventEntity> findPendingForUpdate(@Param("statuses") List<String> statuses,
                                                 @Param("eventTypes") List<String> eventTypes,
                                                 @Param("now") LocalDateTime now,
                                                 Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM outbox_events " +
            "WHERE status_id = :sentStatusId " +
            "  AND created_at < :cutoff",
            nativeQuery = true)
    int deleteSentBefore(@Param("cutoff") LocalDateTime cutoff,
                         @Param("sentStatusId") Long sentStatusId);
}
