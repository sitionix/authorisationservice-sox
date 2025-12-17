package com.sitionix.athssox.postgresql.mapper;

import com.sitionix.athssox.domain.model.UserStatus;
import com.sitionix.athssox.postgresql.entity.UserStatusEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserStatusInfraMapperTest {

    private UserStatusInfraMapper userStatusInfraMapper;

    @BeforeEach
    void setUp() {
        this.userStatusInfraMapper = new UserStatusInfraMapper() {
        };
    }

    @Test
    void givenNullStatusEntity_whenAsStatus_thenReturnNull() {
        //given
        final UserStatusEntity given = null;

        //when
        final UserStatus actual = this.userStatusInfraMapper.asStatus(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void givenStatusEntity_whenAsStatus_thenReturnUserStatus() {
        //given
        final UserStatusEntity given = this.getUserStatusEntity(1L, "PENDING EMAIL VERIFY");
        final UserStatus expected = UserStatus.PENDING_EMAIL_VERIFY;

        //when
        final UserStatus actual = this.userStatusInfraMapper.asStatus(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenNullStatus_whenAsUserStatusEntity_thenReturnNull() {
        //given
        final UserStatus given = null;

        //when
        final UserStatusEntity actual = this.userStatusInfraMapper.asUserStatusEntity(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void givenUserStatus_whenAsUserStatusEntity_thenReturnUserStatusEntity() {
        //given
        final UserStatus given = UserStatus.PENDING_EMAIL_VERIFY;
        final UserStatusEntity expected = this.getUserStatusEntity(1L, "PENDING EMAIL VERIFY");

        //when
        final UserStatusEntity actual = this.userStatusInfraMapper.asUserStatusEntity(given);

        //then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    private UserStatusEntity getUserStatusEntity(final Long id, final String description) {
        return UserStatusEntity.builder()
                .id(id)
                .description(description)
                .build();
    }
}

