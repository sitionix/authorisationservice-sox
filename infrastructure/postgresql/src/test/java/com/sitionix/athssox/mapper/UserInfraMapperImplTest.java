package com.sitionix.athssox.mapper;

import com.sitionix.athssox.domain.RegisterUserDO;
import com.sitionix.athssox.domain.ResponseRegisterUser;
import com.sitionix.athssox.domain.UserRole;
import com.sitionix.athssox.domain.UserStatus;
import com.sitionix.athssox.entity.GlobalRoleEntity;
import com.sitionix.athssox.entity.UserEntity;
import com.sitionix.athssox.entity.UserStatusEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserInfraMapperImplTest {

    private UserInfraMapper userInfraMapper;

    private UserRoleInfraMapper userRoleInfraMapper;

    private UserStatusInfraMapper userStatusInfraMapper;

    @BeforeEach
    void setUp() {
        this.userRoleInfraMapper = new UserRoleInfraMapper() {
        };
        this.userStatusInfraMapper = new UserStatusInfraMapper() {
        };
        this.userInfraMapper = new UserInfraMapperImpl(
                this.userRoleInfraMapper,
                this.userStatusInfraMapper);
    }
    @Test
    void givenResponseRegisterUser_whenAsUserEntity_thenReturnUserEntity() {
        //given
        final UUID siteId = UUID.randomUUID();

        final RegisterUserDO given = this.getRegisterUser(siteId);
        final UserEntity expected = this.getUserEntity(siteId);

        //when
        final UserEntity actual = this.userInfraMapper.asUserEntity(given);

        //then
        assertThat(actual).isEqualTo(expected);

    }

    @Test
    void givenUserEntity_whenResponseRegisterUser_thenReturnResponseRegisterUser() {
        //given
        final UUID siteId = UUID.randomUUID();
        final UserEntity given = this.getUserEntityForResponse(siteId);
        final ResponseRegisterUser expected = this.getResponseRegisterUser();

        //when
        final ResponseRegisterUser actual = this.userInfraMapper.asResponseRegisterUser(given);

        //then
        assertThat(actual).isEqualTo(expected);

    }

    private RegisterUserDO getRegisterUser(final UUID siteId) {
        return RegisterUserDO.builder()
                .email("email@sitionix.com")
                .password("P@ssw0rd!")
                .role(UserRole.SITE_USER)
                .status(UserStatus.PENDING_EMAIL_VERIFY)
                .siteId(siteId)
                .build();
    }

    private ResponseRegisterUser getResponseRegisterUser() {
        return ResponseRegisterUser.builder()
                .userId(1L)
                .status(UserStatus.PENDING_EMAIL_VERIFY)
                .build();
    }

    private UserEntity getUserEntity(final UUID siteId) {
        return UserEntity.builder()
                .email("email@sitionix.com")
                .status(this.getUserStatusEntity())
                .globalRole(this.getGlobalRoleEntity())
                .siteId(siteId)
                .passwordHash("P@ssw0rd!")
                .build();
    }
    private UserEntity getUserEntityForResponse(final UUID siteId) {
        return UserEntity.builder()
                .id(1L)
                .status(this.getUserStatusEntity())
                .email("email@sitionix.com")
                .siteId(siteId)
                .build();
    }

    private UserStatusEntity getUserStatusEntity() {
        return UserStatusEntity.builder()
                .id(1L)
                .description("PENDING EMAIL VERIFY")
                .build();
    }

    private GlobalRoleEntity getGlobalRoleEntity() {
        return GlobalRoleEntity.builder()
                .id(1L)
                .description("SITE USER")
                .build();
    }
}
