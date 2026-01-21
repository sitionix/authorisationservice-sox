package com.sitionix.athssox.postgresql.repository;

import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.domain.repository.AuthUserRepository;
import com.sitionix.athssox.postgresql.entity.user.UserEntity;
import com.sitionix.athssox.postgresql.jpa.user.UserJpaRepository;
import com.sitionix.athssox.postgresql.mapper.user.UserInfraMapper;
import com.sitionix.athssox.postgresql.repository.user.AuthUserRepositoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUserRepositoryImplTest {

    private AuthUserRepository authUserRepository;

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private UserInfraMapper userInfraMapper;

    @BeforeEach
    void setUp() {
        this.authUserRepository = new AuthUserRepositoryImpl(this.userJpaRepository,
                this.userInfraMapper);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.userJpaRepository,
                this.userInfraMapper);
    }

    @Test
    void given_email_and_site_id_when_find_by_email_and_site_id_then_return_auth_user() {
        //given
        final String email = "user@sitionix.com";
        final UUID siteId = UUID.randomUUID();
        final UserEntity userEntity = mock(UserEntity.class);
        final AuthUser expected = mock(AuthUser.class);

        when(this.userJpaRepository.findByEmailAndSiteId(email, siteId))
                .thenReturn(Optional.of(userEntity));
        when(this.userInfraMapper.asAuthUser(userEntity))
                .thenReturn(expected);

        //when
        final Optional<AuthUser> actual = this.authUserRepository.findByEmailAndSiteId(email, siteId);

        //then
        assertThat(actual).isEqualTo(Optional.of(expected));
        verify(this.userJpaRepository)
                .findByEmailAndSiteId(email, siteId);
        verify(this.userInfraMapper)
                .asAuthUser(userEntity);
    }

    @Test
    void given_email_and_site_id_when_find_by_email_and_site_id_empty_then_return_empty() {
        //given
        final String email = "user@sitionix.com";
        final UUID siteId = UUID.randomUUID();

        when(this.userJpaRepository.findByEmailAndSiteId(email, siteId))
                .thenReturn(Optional.empty());

        //when
        final Optional<AuthUser> actual = this.authUserRepository.findByEmailAndSiteId(email, siteId);

        //then
        assertThat(actual).isEmpty();
        verify(this.userJpaRepository)
                .findByEmailAndSiteId(email, siteId);
    }

    @Test
    void given_email_when_find_global_by_email_then_return_auth_user() {
        //given
        final String email = "user@sitionix.com";
        final UserEntity userEntity = mock(UserEntity.class);
        final AuthUser expected = mock(AuthUser.class);

        when(this.userJpaRepository.findByEmailAndSiteIdIsNull(email))
                .thenReturn(Optional.of(userEntity));
        when(this.userInfraMapper.asAuthUser(userEntity))
                .thenReturn(expected);

        //when
        final Optional<AuthUser> actual = this.authUserRepository.findGlobalByEmail(email);

        //then
        assertThat(actual).isEqualTo(Optional.of(expected));
        verify(this.userJpaRepository)
                .findByEmailAndSiteIdIsNull(email);
        verify(this.userInfraMapper)
                .asAuthUser(userEntity);
    }

    @Test
    void given_email_when_exists_site_scoped_by_email_then_return_result() {
        //given
        final String email = "user@sitionix.com";

        when(this.userJpaRepository.existsByEmailAndGlobalRole_IdIn(email, UserRole.siteScopedIds()))
                .thenReturn(true);

        //when
        final boolean actual = this.authUserRepository.existsSiteScopedByEmail(email);

        //then
        assertThat(actual).isTrue();
        verify(this.userJpaRepository)
                .existsByEmailAndGlobalRole_IdIn(email, UserRole.siteScopedIds());
    }

    @Test
    void given_user_id_when_find_by_id_then_return_auth_user() {
        //given
        final Long userId = this.getUserId();
        final UserEntity userEntity = mock(UserEntity.class);
        final AuthUser expected = mock(AuthUser.class);

        when(this.userJpaRepository.findById(userId))
                .thenReturn(Optional.of(userEntity));
        when(this.userInfraMapper.asAuthUser(userEntity))
                .thenReturn(expected);

        //when
        final Optional<AuthUser> actual = this.authUserRepository.findById(userId);

        //then
        assertThat(actual).isEqualTo(Optional.of(expected));
        verify(this.userJpaRepository)
                .findById(userId);
        verify(this.userInfraMapper)
                .asAuthUser(userEntity);
    }

    @Test
    void given_user_id_when_find_by_id_empty_then_return_empty() {
        //given
        final Long userId = this.getUserId();

        when(this.userJpaRepository.findById(userId))
                .thenReturn(Optional.empty());

        //when
        final Optional<AuthUser> actual = this.authUserRepository.findById(userId);

        //then
        assertThat(actual).isEmpty();
        verify(this.userJpaRepository)
                .findById(userId);
    }

    @Test
    void given_auth_user_when_save_then_persist_entity() {
        //given
        final AuthUser given = mock(AuthUser.class);
        final UserEntity userEntity = mock(UserEntity.class);

        when(this.userInfraMapper.asUserEntity(given))
                .thenReturn(userEntity);

        //when
        this.authUserRepository.save(given);

        //then
        verify(this.userInfraMapper)
                .asUserEntity(given);
        verify(this.userJpaRepository)
                .save(userEntity);
    }

    private Long getUserId() {
        return 10L;
    }
}
