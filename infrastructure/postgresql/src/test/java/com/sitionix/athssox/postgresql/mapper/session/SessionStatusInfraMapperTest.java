package com.sitionix.athssox.postgresql.mapper.session;

import com.sitionix.athssox.domain.model.SessionStatus;
import com.sitionix.athssox.postgresql.entity.session.SessionStatusEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SessionStatusInfraMapperTest {

    private SessionStatusInfraMapper sessionStatusInfraMapper;

    @BeforeEach
    void setUp() {
        sessionStatusInfraMapper = new SessionStatusInfraMapperImpl();
    }

    @Test
    void givenSessionStatusEntity_whenAsSessionStatus_thenReturnSessionStatus() {
        // Given
        final SessionStatusEntity given = this.givenSessionStatusEntity();
        final SessionStatus expected = SessionStatus.ACTIVE;

        // When
        final SessionStatus actual = sessionStatusInfraMapper.asStatus(given);

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenSessionStatus_whenAsSessionStatusEntity_thenReturnSessionStatusEntity() {
        // Given
        final SessionStatus given = SessionStatus.ACTIVE;
        final SessionStatusEntity expected = this.givenSessionStatusEntity();

        // When
        final SessionStatusEntity actual = sessionStatusInfraMapper.asSessionStatusEntity(given);

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    private SessionStatusEntity givenSessionStatusEntity() {
        return SessionStatusEntity.builder()
                .id(1L)
                .description("ACTIVE")
                .build();
    }

}