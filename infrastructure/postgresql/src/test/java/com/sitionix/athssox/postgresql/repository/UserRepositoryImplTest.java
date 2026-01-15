package com.sitionix.athssox.postgresql.repository;

import com.sitionix.athssox.domain.model.RegisterUserDO;
import com.sitionix.athssox.domain.model.ResponseRegisterUser;
import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.domain.repository.UserRepository;
import com.sitionix.athssox.postgresql.entity.user.UserEntity;
import com.sitionix.athssox.postgresql.jpa.user.UserJpaRepository;
import com.sitionix.athssox.postgresql.mapper.user.UserInfraMapper;
import com.sitionix.athssox.postgresql.repository.user.UserRepositoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    private UserRepository userRepository;

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private UserInfraMapper userInfraMapper;

    @BeforeEach
    void setUp() {
        this.userRepository = new UserRepositoryImpl(
                this.userJpaRepository,
                this.userInfraMapper
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.userJpaRepository,
                this.userInfraMapper);
    }

    @Test
    void givenRegisterUserDO_whenCreateUser_thenReturnCreatedUser() {
        //given
        final RegisterUserDO given = mock(RegisterUserDO.class);
        final UserEntity mappedUserEntity = mock(UserEntity.class);
        final UserEntity createdUserEntity = mock(UserEntity.class);
        final ResponseRegisterUser expected = mock(ResponseRegisterUser.class);

        when(this.userInfraMapper.asUserEntity(given))
                .thenReturn(mappedUserEntity);
        when(this.userJpaRepository.save(mappedUserEntity))
                .thenReturn(createdUserEntity);
        when(this.userInfraMapper.asResponseRegisterUser(createdUserEntity))
                .thenReturn(expected);

        //when
        final ResponseRegisterUser actual = this.userRepository.createUser(given);

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.userInfraMapper)
                .asUserEntity(given);
        verify(this.userJpaRepository)
                .save(mappedUserEntity);
        verify(this.userInfraMapper)
                .asResponseRegisterUser(createdUserEntity);
    }

    @Test
    void givenEmailAndSiteId_whenExistsSiteScopedByEmailAndSiteId_thenReturnTrue() {
        //given
        final String email = "email@sitionix.com";
        final UUID siteId = this.getSiteId();
        final List<Long> roleIds = this.getSiteScopedRoleIds();

        when(this.userJpaRepository.existsByEmailAndSiteIdAndGlobalRole_IdIn(email, siteId, roleIds))
                .thenReturn(true);

        //when
        final boolean actual = this.userRepository.existsSiteScopedByEmailAndSiteId(email, siteId);

        //then
        assertThat(actual).isTrue();
        verify(this.userJpaRepository)
                .existsByEmailAndSiteIdAndGlobalRole_IdIn(email, siteId, roleIds);
    }

    @Test
    void givenEmail_whenExistsGlobalByEmail_thenReturnFalse() {
        //given
        final String email = "email@sitionix.com";
        final List<Long> roleIds = this.getGlobalRoleIds();

        when(this.userJpaRepository.existsByEmailAndGlobalRole_IdIn(email, roleIds))
                .thenReturn(false);

        //when
        final boolean actual = this.userRepository.existsGlobalByEmail(email);

        //then
        assertThat(actual).isFalse();
        verify(this.userJpaRepository)
                .existsByEmailAndGlobalRole_IdIn(email, roleIds);
    }

    @Test
    void givenEmailAndSiteId_whenFindSiteScopedByEmailAndSiteId_thenReturnUser() {
        //given
        final String email = "email@sitionix.com";
        final UUID siteId = this.getSiteId();
        final List<Long> roleIds = this.getSiteScopedRoleIds();
        final UserEntity entity = mock(UserEntity.class);
        final ResponseRegisterUser expected = mock(ResponseRegisterUser.class);

        when(this.userJpaRepository.findByEmailAndSiteIdAndGlobalRole_IdIn(email, siteId, roleIds))
                .thenReturn(Optional.of(entity));
        when(this.userInfraMapper.asResponseRegisterUser(entity))
                .thenReturn(expected);

        //when
        final Optional<ResponseRegisterUser> actual =
                this.userRepository.findSiteScopedByEmailAndSiteId(email, siteId);

        //then
        assertThat(actual).isEqualTo(Optional.of(expected));
        verify(this.userJpaRepository)
                .findByEmailAndSiteIdAndGlobalRole_IdIn(email, siteId, roleIds);
        verify(this.userInfraMapper)
                .asResponseRegisterUser(entity);
    }

    @Test
    void givenEmail_whenFindGlobalByEmail_thenReturnUser() {
        //given
        final String email = "email@sitionix.com";
        final List<Long> roleIds = this.getGlobalRoleIds();
        final UserEntity entity = mock(UserEntity.class);
        final ResponseRegisterUser expected = mock(ResponseRegisterUser.class);

        when(this.userJpaRepository.findByEmailAndSiteIdIsNullAndGlobalRole_IdIn(email, roleIds))
                .thenReturn(Optional.of(entity));
        when(this.userInfraMapper.asResponseRegisterUser(entity))
                .thenReturn(expected);

        //when
        final Optional<ResponseRegisterUser> actual =
                this.userRepository.findGlobalByEmail(email);

        //then
        assertThat(actual).isEqualTo(Optional.of(expected));
        verify(this.userJpaRepository)
                .findByEmailAndSiteIdIsNullAndGlobalRole_IdIn(email, roleIds);
        verify(this.userInfraMapper)
                .asResponseRegisterUser(entity);
    }

    private UUID getSiteId() {
        return UUID.randomUUID();
    }

    private List<Long> getSiteScopedRoleIds() {
        return List.of(UserRole.SITE_USER.getId(),
                UserRole.SITE_ADMIN.getId());
    }

    private List<Long> getGlobalRoleIds() {
        return List.of(UserRole.SUPER_ADMIN.getId(),
                UserRole.ECOSYSTEM_OWNER.getId());
    }
}
