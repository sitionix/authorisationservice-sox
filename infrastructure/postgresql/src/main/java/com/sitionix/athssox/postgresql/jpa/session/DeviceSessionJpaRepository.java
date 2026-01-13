package com.sitionix.athssox.postgresql.jpa.session;

import com.sitionix.athssox.postgresql.entity.session.DeviceSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeviceSessionJpaRepository extends JpaRepository<DeviceSessionEntity, UUID> {

    Optional<DeviceSessionEntity> findByUser_IdAndSessionSourceId(final Long userId, final String sessionSourceId);
}
