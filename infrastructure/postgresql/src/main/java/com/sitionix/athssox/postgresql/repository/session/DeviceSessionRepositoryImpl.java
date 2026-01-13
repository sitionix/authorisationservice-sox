package com.sitionix.athssox.postgresql.repository.session;

import com.sitionix.athssox.domain.model.DeviceSession;
import com.sitionix.athssox.domain.repository.DeviceSessionRepository;
import com.sitionix.athssox.postgresql.entity.session.DeviceSessionEntity;
import com.sitionix.athssox.postgresql.jpa.session.DeviceSessionJpaRepository;
import com.sitionix.athssox.postgresql.mapper.session.DeviceSessionInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceSessionRepositoryImpl implements DeviceSessionRepository {

    private final DeviceSessionJpaRepository deviceSessionJpaRepository;
    private final DeviceSessionInfraMapper deviceSessionInfraMapper;

    @Override
    public Optional<DeviceSession> findByUserIdAndSessionSourceId(final Long userId, final String sessionSourceId) {
        return this.deviceSessionJpaRepository.findByUser_IdAndSessionSourceId(userId, sessionSourceId)
                .map(this.deviceSessionInfraMapper::asDeviceSession);
    }

    @Override
    public DeviceSession save(final DeviceSession deviceSession) {
        final DeviceSessionEntity entity = this.deviceSessionInfraMapper.asDeviceSessionEntity(deviceSession);
        final DeviceSessionEntity saved = this.deviceSessionJpaRepository.save(entity);
        return this.deviceSessionInfraMapper.asDeviceSession(saved);
    }
}
