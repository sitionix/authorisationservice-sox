package com.sitionix.athssox.domain.repository;

import com.sitionix.athssox.domain.model.DeviceSession;

import java.util.Optional;

public interface DeviceSessionRepository {

    Optional<DeviceSession> findByUserIdAndSessionSourceId(final Long userId, final String sessionSourceId);

    DeviceSession save(final DeviceSession deviceSession);
}
