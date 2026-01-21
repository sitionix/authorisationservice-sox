package com.sitionix.athssox.application.security;

import com.sitionix.athssox.domain.exception.MissingSiteIdException;
import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.domain.model.UserStatus;
import com.sitionix.athssox.domain.repository.AuthUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAuthenticationProviderTest {

    private AuthenticationProvider loginAuthenticationProvider;

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        this.loginAuthenticationProvider = new LoginAuthenticationProvider(this.authUserRepository,
                this.passwordEncoder);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.authUserRepository,
                this.passwordEncoder);
    }

    @Test
    void given_active_user_when_authenticate_then_return_authenticated_token() {
        //given
        final UUID siteId = UUID.randomUUID();
        final String email = "user@sitionix.com";
        final String password = "StrongPassword123";
        final AuthUser user = this.getAuthUser(10L, siteId, UserStatus.ACTIVE);
        final LoginAuthenticationToken expected = LoginAuthenticationToken.authenticated(user);
        final Authentication given = LoginAuthenticationToken.unauthenticated(email, password, siteId);

        when(this.authUserRepository.findByEmailAndSiteId(email, siteId))
                .thenReturn(Optional.of(user));
        when(this.passwordEncoder.matches(password, user.getPasswordHash()))
                .thenReturn(true);

        //when
        final Authentication actual = this.loginAuthenticationProvider.authenticate(given);

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.authUserRepository)
                .findByEmailAndSiteId(email, siteId);
        verify(this.passwordEncoder)
                .matches(password, user.getPasswordHash());
    }

    @Test
    void given_site_scoped_missing_when_authenticate_then_fallback_to_global_user() {
        //given
        final UUID siteId = UUID.randomUUID();
        final String email = "user@sitionix.com";
        final String password = "StrongPassword123";
        final AuthUser user = this.getAuthUser(22L, null, UserStatus.ACTIVE);
        final LoginAuthenticationToken expected = LoginAuthenticationToken.authenticated(user);
        final Authentication given = LoginAuthenticationToken.unauthenticated(email, password, siteId);

        when(this.authUserRepository.findByEmailAndSiteId(email, siteId))
                .thenReturn(Optional.empty());
        when(this.authUserRepository.findGlobalByEmail(email))
                .thenReturn(Optional.of(user));
        when(this.passwordEncoder.matches(password, user.getPasswordHash()))
                .thenReturn(true);

        //when
        final Authentication actual = this.loginAuthenticationProvider.authenticate(given);

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.authUserRepository)
                .findByEmailAndSiteId(email, siteId);
        verify(this.authUserRepository)
                .findGlobalByEmail(email);
        verify(this.passwordEncoder)
                .matches(password, user.getPasswordHash());
    }

    @Test
    void given_inactive_user_when_authenticate_then_throw_bad_credentials_exception() {
        //given
        final String email = "user@sitionix.com";
        final String password = "StrongPassword123";
        final AuthUser user = this.getAuthUser(12L, null, UserStatus.PENDING_EMAIL_VERIFY);
        final Authentication given = LoginAuthenticationToken.unauthenticated(email, password, null);

        when(this.authUserRepository.findGlobalByEmail(email))
                .thenReturn(Optional.of(user));

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.loginAuthenticationProvider.authenticate(given));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
        verify(this.authUserRepository)
                .findGlobalByEmail(email);
    }

    @Test
    void given_invalid_password_when_authenticate_then_throw_bad_credentials_exception() {
        //given
        final String email = "user@sitionix.com";
        final String password = "StrongPassword123";
        final AuthUser user = this.getAuthUser(14L, null, UserStatus.ACTIVE);
        final Authentication given = LoginAuthenticationToken.unauthenticated(email, password, null);

        when(this.authUserRepository.findGlobalByEmail(email))
                .thenReturn(Optional.of(user));
        when(this.passwordEncoder.matches(password, user.getPasswordHash()))
                .thenReturn(false);

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.loginAuthenticationProvider.authenticate(given));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
        verify(this.authUserRepository)
                .findGlobalByEmail(email);
        verify(this.passwordEncoder)
                .matches(password, user.getPasswordHash());
    }

    @Test
    void given_site_scoped_user_and_missing_site_id_when_authenticate_then_throw_missing_site_id_exception() {
        //given
        final String email = "user@sitionix.com";
        final String password = "StrongPassword123";
        final Authentication given = LoginAuthenticationToken.unauthenticated(email, password, null);

        when(this.authUserRepository.findGlobalByEmail(email))
                .thenReturn(Optional.empty());
        when(this.authUserRepository.existsSiteScopedByEmail(email))
                .thenReturn(true);

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.loginAuthenticationProvider.authenticate(given));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(MissingSiteIdException.class)
                .hasMessage("siteId is required for site-scoped roles");
        verify(this.authUserRepository)
                .findGlobalByEmail(email);
        verify(this.authUserRepository)
                .existsSiteScopedByEmail(email);
        verifyNoInteractions(this.passwordEncoder);
    }

    @Test
    void given_authentication_class_when_supports_then_return_true() {
        //given
        final Class<?> given = LoginAuthenticationToken.class;

        //when
        final boolean actual = this.loginAuthenticationProvider.supports(given);

        //then
        assertThat(actual).isTrue();
    }

    private AuthUser getAuthUser(final Long id, final UUID siteId, final UserStatus status) {
        return AuthUser.builder()
                .id(id)
                .email("user@sitionix.com")
                .passwordHash("hashed")
                .status(status)
                .role(UserRole.SITE_USER)
                .siteId(siteId)
                .build();
    }
}
