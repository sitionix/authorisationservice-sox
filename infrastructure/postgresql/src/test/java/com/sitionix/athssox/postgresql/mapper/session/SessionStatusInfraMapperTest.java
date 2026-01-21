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
    void given_session_status_entity_when_as_session_status_then_return_session_status() {
        // Given
        final SessionStatusEntity given = this.givenSessionStatusEntity();
        final SessionStatus expected = SessionStatus.ACTIVE;

        // When
        final SessionStatus actual = sessionStatusInfraMapper.asStatus(given);

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_session_status_when_as_session_status_entity_then_return_session_status_entity() {
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