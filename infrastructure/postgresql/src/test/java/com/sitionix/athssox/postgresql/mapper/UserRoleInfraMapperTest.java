package com.sitionix.athssox.postgresql.mapper;

import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.postgresql.entity.user.GlobalRoleEntity;
import com.sitionix.athssox.postgresql.mapper.user.UserRoleInfraMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserRoleInfraMapperTest {

    private UserRoleInfraMapper userRoleInfraMapper;

    @BeforeEach
    void setUp() {
        this.userRoleInfraMapper = new UserRoleInfraMapper() {
        };
    }

    @Test
    void givenNullUserRole_whenAsGlobalRoleEntity_thenReturnNull() {
        //given
        final UserRole given = null;

        //when
        final GlobalRoleEntity actual = this.userRoleInfraMapper.asGlobalRoleEntity(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void givenUserRole_whenAsGlobalRoleEntity_thenReturnGlobalRoleEntity() {
        //given
        final UserRole given = UserRole.SITE_USER;
        final GlobalRoleEntity expected = this.getGlobalRoleEntity(1L, "SITE USER");

        //when
        final GlobalRoleEntity actual = this.userRoleInfraMapper.asGlobalRoleEntity(given);

        //then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenNullGlobalRoleEntity_whenAsUserRole_thenReturnNull() {
        //given
        final GlobalRoleEntity given = null;

        //when
        final UserRole actual = this.userRoleInfraMapper.asUserRole(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void givenGlobalRoleEntity_whenAsUserRole_thenReturnUserRole() {
        //given
        final GlobalRoleEntity given = this.getGlobalRoleEntity(4L, "SITE ADMIN");
        final UserRole expected = UserRole.SITE_ADMIN;

        //when
        final UserRole actual = this.userRoleInfraMapper.asUserRole(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private GlobalRoleEntity getGlobalRoleEntity(final Long id, final String description) {
        return GlobalRoleEntity.builder()
                .id(id)
                .description(description)
                .build();
    }
}
