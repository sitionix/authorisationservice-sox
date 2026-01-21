package com.sitionix.athssox.postgresql.mapper.token;

import com.sitionix.athssox.domain.model.RefreshTokenStatus;
import com.sitionix.athssox.postgresql.entity.token.RefreshTokenStatusEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RefreshTokenStatusInfraMapperTest {

    private RefreshTokenStatusInfraMapper refreshTokenStatusInfraMapper;

    @BeforeEach
    void setUp() {
        this.refreshTokenStatusInfraMapper = new RefreshTokenStatusInfraMapper() {
        };
    }

    @Test
    void given_null_status_entity_when_as_status_then_return_null() {
        //given
        final RefreshTokenStatusEntity given = null;

        //when
        final RefreshTokenStatus actual = this.refreshTokenStatusInfraMapper.asStatus(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void given_status_entity_when_as_status_then_return_status() {
        //given
        final RefreshTokenStatusEntity given = this.getStatusEntity(1L, "ACTIVE");
        final RefreshTokenStatus expected = RefreshTokenStatus.ACTIVE;

        //when
        final RefreshTokenStatus actual = this.refreshTokenStatusInfraMapper.asStatus(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_null_status_when_as_status_entity_then_return_null() {
        //given
        final RefreshTokenStatus given = null;

        //when
        final RefreshTokenStatusEntity actual = this.refreshTokenStatusInfraMapper.asStatusEntity(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void given_status_when_as_status_entity_then_return_status_entity() {
        //given
        final RefreshTokenStatus given = RefreshTokenStatus.ACTIVE;
        final RefreshTokenStatusEntity expected = this.getStatusEntity(1L, "ACTIVE");

        //when
        final RefreshTokenStatusEntity actual = this.refreshTokenStatusInfraMapper.asStatusEntity(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private RefreshTokenStatusEntity getStatusEntity(final Long id, final String description) {
        return RefreshTokenStatusEntity.builder()
                .id(id)
                .description(description)
                .build();
    }
}
