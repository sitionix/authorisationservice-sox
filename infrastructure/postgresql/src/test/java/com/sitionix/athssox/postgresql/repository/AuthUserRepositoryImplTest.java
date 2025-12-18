package com.sitionix.athssox.postgresql.repository;

import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.repository.AuthUserRepository;
import com.sitionix.athssox.postgresql.entity.UserEntity;
import com.sitionix.athssox.postgresql.jpa.UserJpaRepository;
import com.sitionix.athssox.postgresql.mapper.UserInfraMapper;
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
    void givenEmailAndSiteId_whenFindByEmailAndSiteId_thenReturnAuthUser() {
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
    void givenEmailAndSiteId_whenFindByEmailAndSiteIdEmpty_thenReturnEmpty() {
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
    void givenEmail_whenFindGlobalByEmail_thenReturnAuthUser() {
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
}
