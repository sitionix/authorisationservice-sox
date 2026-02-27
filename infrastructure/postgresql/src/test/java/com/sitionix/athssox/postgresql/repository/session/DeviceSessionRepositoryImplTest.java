package com.sitionix.athssox.postgresql.repository.session;

import com.sitionix.athssox.domain.model.DeviceSession;
import com.sitionix.athssox.domain.repository.DeviceSessionRepository;
import com.sitionix.athssox.postgresql.entity.session.DeviceSessionEntity;
import com.sitionix.athssox.postgresql.jpa.session.DeviceSessionJpaRepository;
import com.sitionix.athssox.postgresql.mapper.session.DeviceSessionInfraMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceSessionRepositoryImplTest {

    private DeviceSessionRepository repository;

    @Mock
    private DeviceSessionJpaRepository deviceSessionJpaRepository;

    @Mock
    private DeviceSessionInfraMapper deviceSessionInfraMapper;

    @BeforeEach
    void setUp() {
        this.repository = new DeviceSessionRepositoryImpl(
                this.deviceSessionJpaRepository,
                this.deviceSessionInfraMapper
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
                this.deviceSessionJpaRepository,
                this.deviceSessionInfraMapper
        );
    }

    @Test
    void givenUserIdAndSessionSourceId_whenFindByUserIdAndSessionSourceId_thenDeviceSession() {
        // Given
        final Long userId = 1L;
        final String sessionSourceId = "session-source-id";

        final DeviceSessionEntity deviceSessionEntity = mock(DeviceSessionEntity.class);
        final DeviceSession deviceSession = mock(DeviceSession.class);

         when(this.deviceSessionJpaRepository.findByUser_IdAndSessionSourceId(userId, sessionSourceId))
                .thenReturn(Optional.of(deviceSessionEntity));

        when(this.deviceSessionInfraMapper.asDeviceSession(deviceSessionEntity))
                .thenReturn(deviceSession);

        // When
        final Optional<DeviceSession> actual = this.repository.findByUserIdAndSessionSourceId(userId, sessionSourceId);

        // Then
        assertThat(actual).isEqualTo(Optional.of(deviceSession));
        verify(this.deviceSessionJpaRepository).findByUser_IdAndSessionSourceId(userId, sessionSourceId);
    }

    @Test
    void givenDeviceSession_whenSave_thenSavedDeviceSession() {
        // Given
        final DeviceSession deviceSession = mock(DeviceSession.class);
        final DeviceSessionEntity deviceSessionEntity = mock(DeviceSessionEntity.class);
        final DeviceSessionEntity savedEntity = mock(DeviceSessionEntity.class);
        final DeviceSession savedDeviceSession = mock(DeviceSession.class);

        when(this.deviceSessionInfraMapper.asDeviceSessionEntity(deviceSession))
                .thenReturn(deviceSessionEntity);

        when(this.deviceSessionJpaRepository.save(deviceSessionEntity))
                .thenReturn(savedEntity);

        when(this.deviceSessionInfraMapper.asDeviceSession(savedEntity))
                .thenReturn(savedDeviceSession);

        // When
        final DeviceSession actual = this.repository.save(deviceSession);

        // Then
        assertThat(actual).isEqualTo(savedDeviceSession);

        verify(this.deviceSessionInfraMapper).asDeviceSessionEntity(deviceSession);
        verify(this.deviceSessionJpaRepository).save(deviceSessionEntity);
        verify(this.deviceSessionInfraMapper).asDeviceSession(savedEntity);
    }

}