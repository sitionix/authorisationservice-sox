package com.sitionix.athssox.application.usecase;

import com.sitionix.athssox.application.config.SessionConfig;
import com.sitionix.athssox.application.security.LoginAuthenticationToken;
import com.sitionix.athssox.domain.model.AccessToken;
import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.model.DeviceSession;
import com.sitionix.athssox.domain.model.LoginRequest;
import com.sitionix.athssox.domain.model.LoginResponse;
import com.sitionix.athssox.domain.model.RefreshToken;
import com.sitionix.athssox.domain.model.RefreshTokenRecord;
import com.sitionix.athssox.domain.model.RefreshTokenStatus;
import com.sitionix.athssox.domain.model.SessionStatus;
import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.domain.model.UserStatus;
import com.sitionix.athssox.domain.repository.DeviceSessionRepository;
import com.sitionix.athssox.domain.repository.RefreshTokenRepository;
import com.sitionix.athssox.domain.service.TokenHasher;
import com.sitionix.athssox.domain.service.TokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUserImplTest {

    private static final Instant NOW = Instant.parse("2024-05-01T10:15:30Z");

    private LoginUserImpl loginUser;
    private SessionConfig sessionConfig;

    @Mock
    private DeviceSessionRepository deviceSessionRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private TokenHasher tokenHasher;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Clock clock;

    @BeforeEach
    void setUp() {
        this.sessionConfig = this.getSessionConfig(5L);
        this.loginUser = new LoginUserImpl(this.deviceSessionRepository,
                this.refreshTokenRepository,
                this.tokenProvider,
                this.tokenHasher,
                this.authenticationManager,
                this.clock,
                this.sessionConfig);
    }

    @AfterEach
    void tearDown() {
        verify(this.refreshTokenRepository, atMostOnce())
                .revokeActiveBySessionId(any(UUID.class), eq(NOW), eq("ROTATED"));
        verifyNoMoreInteractions(this.deviceSessionRepository,
                this.refreshTokenRepository,
                this.tokenProvider,
                this.tokenHasher,
                this.authenticationManager,
                this.clock);
    }

    @Test
    void givenValidLogin_whenExecute_thenReturnTokensAndPersistRefreshToken() {
        //given
        final UUID siteId = UUID.randomUUID();
        final LoginRequest given = this.getLoginRequest(siteId);
        final AuthUser user = this.getAuthUser(10L, siteId);
        final AccessToken accessToken = this.getAccessToken("access-token", NOW.plusSeconds(3600));
        final RefreshToken refreshToken = this.getRefreshToken("refresh-token", NOW.plusSeconds(7200));
        final LoginResponse expected = this.getLoginResponse(accessToken.getToken(),
                refreshToken.getToken(),
                3600L,
                "Bearer");

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.authenticationManager.authenticate(any(LoginAuthenticationToken.class)))
                .thenReturn(LoginAuthenticationToken.authenticated(user));
        when(this.deviceSessionRepository.findByUserIdAndSessionSourceId(user.getId(), given.getSessionSourceId()))
                .thenReturn(Optional.empty());
        when(this.deviceSessionRepository.save(any(DeviceSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(this.tokenProvider.generateAccessToken(user))
                .thenReturn(accessToken);
        when(this.tokenProvider.generateRefreshToken(user))
                .thenReturn(refreshToken);
        when(this.tokenHasher.hash(refreshToken.getToken()))
                .thenReturn("hashed");

        //when
        final LoginResponse actual = this.loginUser.execute(given);

        //then
        final ArgumentCaptor<LoginAuthenticationToken> tokenCaptor = ArgumentCaptor.forClass(LoginAuthenticationToken.class);
        final ArgumentCaptor<RefreshTokenRecord> recordCaptor = ArgumentCaptor.forClass(RefreshTokenRecord.class);
        final ArgumentCaptor<DeviceSession> sessionCaptor = ArgumentCaptor.forClass(DeviceSession.class);

        verify(this.clock)
                .instant();
        verify(this.authenticationManager)
                .authenticate(tokenCaptor.capture());
        verify(this.deviceSessionRepository)
                .findByUserIdAndSessionSourceId(user.getId(), given.getSessionSourceId());
        verify(this.deviceSessionRepository)
                .save(sessionCaptor.capture());
        verify(this.tokenProvider)
                .generateAccessToken(user);
        verify(this.tokenProvider)
                .generateRefreshToken(user);
        verify(this.tokenHasher)
                .hash(refreshToken.getToken());
        verify(this.refreshTokenRepository)
                .save(recordCaptor.capture());

        final LoginAuthenticationToken actualToken = tokenCaptor.getValue();
        assertThat(actualToken.getEmail()).isEqualTo(given.getEmail());
        assertThat(actualToken.getCredentials()).isEqualTo(given.getPassword());
        assertThat(actualToken.getSiteId()).isEqualTo(given.getSiteId());

        final DeviceSession actualSession = sessionCaptor.getValue();
        final DeviceSession expectedSession = this.getDeviceSession(actualSession.getId(),
                user,
                given.getSessionSourceId(),
                SessionStatus.ACTIVE,
                NOW,
                NOW,
                given.getUserAgent(),
                given.getUserAgent());

        assertThat(actualSession).isEqualTo(expectedSession);

        final RefreshTokenRecord expectedRecord = this.getRefreshTokenRecord("hashed",
                user,
                actualSession,
                RefreshTokenStatus.ACTIVE,
                refreshToken.getExpiresAt(),
                NOW,
                NOW);

        assertThat(recordCaptor.getValue()).isEqualTo(expectedRecord);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenExistingSessionWithinThrottleInterval_whenExecute_thenSkipSessionUpdate() {
        //given
        final UUID siteId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        final LoginRequest given = this.getLoginRequest(siteId);
        final AuthUser user = this.getAuthUser(10L, siteId);
        final AccessToken accessToken = this.getAccessToken("access-token", NOW.plusSeconds(3600));
        final RefreshToken refreshToken = this.getRefreshToken("refresh-token", NOW.plusSeconds(7200));
        final LoginResponse expected = this.getLoginResponse(accessToken.getToken(),
                refreshToken.getToken(),
                3600L,
                "Bearer");
        final DeviceSession existingSession = this.getDeviceSession(sessionId,
                user,
                given.getSessionSourceId(),
                SessionStatus.ACTIVE,
                NOW.minusSeconds(3600),
                NOW.minusSeconds(60),
                given.getUserAgent(),
                given.getUserAgent());

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.authenticationManager.authenticate(any(LoginAuthenticationToken.class)))
                .thenReturn(LoginAuthenticationToken.authenticated(user));
        when(this.deviceSessionRepository.findByUserIdAndSessionSourceId(user.getId(), given.getSessionSourceId()))
                .thenReturn(Optional.of(existingSession));
        when(this.tokenProvider.generateAccessToken(user))
                .thenReturn(accessToken);
        when(this.tokenProvider.generateRefreshToken(user))
                .thenReturn(refreshToken);
        when(this.tokenHasher.hash(refreshToken.getToken()))
                .thenReturn("hashed");

        //when
        final LoginResponse actual = this.loginUser.execute(given);

        //then
        final ArgumentCaptor<LoginAuthenticationToken> tokenCaptor = ArgumentCaptor.forClass(LoginAuthenticationToken.class);
        final ArgumentCaptor<RefreshTokenRecord> recordCaptor = ArgumentCaptor.forClass(RefreshTokenRecord.class);

        verify(this.clock)
                .instant();
        verify(this.authenticationManager)
                .authenticate(tokenCaptor.capture());
        verify(this.deviceSessionRepository)
                .findByUserIdAndSessionSourceId(user.getId(), given.getSessionSourceId());
        verify(this.deviceSessionRepository, times(0))
                .save(any(DeviceSession.class));
        verify(this.tokenProvider)
                .generateAccessToken(user);
        verify(this.tokenProvider)
                .generateRefreshToken(user);
        verify(this.tokenHasher)
                .hash(refreshToken.getToken());
        verify(this.refreshTokenRepository)
                .save(recordCaptor.capture());

        final LoginAuthenticationToken actualToken = tokenCaptor.getValue();
        assertThat(actualToken.getEmail()).isEqualTo(given.getEmail());
        assertThat(actualToken.getCredentials()).isEqualTo(given.getPassword());
        assertThat(actualToken.getSiteId()).isEqualTo(given.getSiteId());

        final RefreshTokenRecord expectedRecord = this.getRefreshTokenRecord("hashed",
                user,
                existingSession,
                RefreshTokenStatus.ACTIVE,
                refreshToken.getExpiresAt(),
                NOW,
                NOW);

        assertThat(recordCaptor.getValue()).isEqualTo(expectedRecord);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenExistingSessionWithNullLastUsedAt_whenExecute_thenUpdateSession() {
        //given
        final UUID siteId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        final LoginRequest given = this.getLoginRequest(siteId);
        final AuthUser user = this.getAuthUser(10L, siteId);
        final AccessToken accessToken = this.getAccessToken("access-token", NOW.plusSeconds(3600));
        final RefreshToken refreshToken = this.getRefreshToken("refresh-token", NOW.plusSeconds(7200));
        final LoginResponse expected = this.getLoginResponse(accessToken.getToken(),
                refreshToken.getToken(),
                3600L,
                "Bearer");
        final DeviceSession existingSession = this.getDeviceSession(sessionId,
                user,
                given.getSessionSourceId(),
                SessionStatus.ACTIVE,
                NOW.minusSeconds(3600),
                null,
                given.getUserAgent(),
                given.getUserAgent());

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.authenticationManager.authenticate(any(LoginAuthenticationToken.class)))
                .thenReturn(LoginAuthenticationToken.authenticated(user));
        when(this.deviceSessionRepository.findByUserIdAndSessionSourceId(user.getId(), given.getSessionSourceId()))
                .thenReturn(Optional.of(existingSession));
        when(this.deviceSessionRepository.save(any(DeviceSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(this.tokenProvider.generateAccessToken(user))
                .thenReturn(accessToken);
        when(this.tokenProvider.generateRefreshToken(user))
                .thenReturn(refreshToken);
        when(this.tokenHasher.hash(refreshToken.getToken()))
                .thenReturn("hashed");

        //when
        final LoginResponse actual = this.loginUser.execute(given);

        //then
        final ArgumentCaptor<LoginAuthenticationToken> tokenCaptor = ArgumentCaptor.forClass(LoginAuthenticationToken.class);
        final ArgumentCaptor<RefreshTokenRecord> recordCaptor = ArgumentCaptor.forClass(RefreshTokenRecord.class);
        final ArgumentCaptor<DeviceSession> sessionCaptor = ArgumentCaptor.forClass(DeviceSession.class);

        verify(this.clock)
                .instant();
        verify(this.authenticationManager)
                .authenticate(tokenCaptor.capture());
        verify(this.deviceSessionRepository)
                .findByUserIdAndSessionSourceId(user.getId(), given.getSessionSourceId());
        verify(this.deviceSessionRepository)
                .save(sessionCaptor.capture());
        verify(this.tokenProvider)
                .generateAccessToken(user);
        verify(this.tokenProvider)
                .generateRefreshToken(user);
        verify(this.tokenHasher)
                .hash(refreshToken.getToken());
        verify(this.refreshTokenRepository)
                .save(recordCaptor.capture());

        final LoginAuthenticationToken actualToken = tokenCaptor.getValue();
        assertThat(actualToken.getEmail()).isEqualTo(given.getEmail());
        assertThat(actualToken.getCredentials()).isEqualTo(given.getPassword());
        assertThat(actualToken.getSiteId()).isEqualTo(given.getSiteId());

        final DeviceSession expectedSession = this.getDeviceSession(sessionId,
                user,
                given.getSessionSourceId(),
                SessionStatus.ACTIVE,
                NOW.minusSeconds(3600),
                NOW,
                given.getUserAgent(),
                given.getUserAgent());

        assertThat(sessionCaptor.getValue()).isEqualTo(expectedSession);

        final RefreshTokenRecord expectedRecord = this.getRefreshTokenRecord("hashed",
                user,
                expectedSession,
                RefreshTokenStatus.ACTIVE,
                refreshToken.getExpiresAt(),
                NOW,
                NOW);

        assertThat(recordCaptor.getValue()).isEqualTo(expectedRecord);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenThrottleIntervalZero_whenExecute_thenUpdateSession() {
        //given
        final UUID siteId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        final LoginRequest given = this.getLoginRequest(siteId);
        final AuthUser user = this.getAuthUser(10L, siteId);
        final AccessToken accessToken = this.getAccessToken("access-token", NOW.plusSeconds(3600));
        final RefreshToken refreshToken = this.getRefreshToken("refresh-token", NOW.plusSeconds(7200));
        final LoginResponse expected = this.getLoginResponse(accessToken.getToken(),
                refreshToken.getToken(),
                3600L,
                "Bearer");
        final DeviceSession existingSession = this.getDeviceSession(sessionId,
                user,
                given.getSessionSourceId(),
                SessionStatus.ACTIVE,
                NOW.minusSeconds(3600),
                NOW.minusSeconds(60),
                given.getUserAgent(),
                given.getUserAgent());

        this.sessionConfig.setLastUsedThrottleMinutes(0L);

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.authenticationManager.authenticate(any(LoginAuthenticationToken.class)))
                .thenReturn(LoginAuthenticationToken.authenticated(user));
        when(this.deviceSessionRepository.findByUserIdAndSessionSourceId(user.getId(), given.getSessionSourceId()))
                .thenReturn(Optional.of(existingSession));
        when(this.deviceSessionRepository.save(any(DeviceSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(this.tokenProvider.generateAccessToken(user))
                .thenReturn(accessToken);
        when(this.tokenProvider.generateRefreshToken(user))
                .thenReturn(refreshToken);
        when(this.tokenHasher.hash(refreshToken.getToken()))
                .thenReturn("hashed");

        //when
        final LoginResponse actual = this.loginUser.execute(given);

        //then
        final ArgumentCaptor<LoginAuthenticationToken> tokenCaptor = ArgumentCaptor.forClass(LoginAuthenticationToken.class);
        final ArgumentCaptor<RefreshTokenRecord> recordCaptor = ArgumentCaptor.forClass(RefreshTokenRecord.class);
        final ArgumentCaptor<DeviceSession> sessionCaptor = ArgumentCaptor.forClass(DeviceSession.class);

        verify(this.clock)
                .instant();
        verify(this.authenticationManager)
                .authenticate(tokenCaptor.capture());
        verify(this.deviceSessionRepository)
                .findByUserIdAndSessionSourceId(user.getId(), given.getSessionSourceId());
        verify(this.deviceSessionRepository)
                .save(sessionCaptor.capture());
        verify(this.tokenProvider)
                .generateAccessToken(user);
        verify(this.tokenProvider)
                .generateRefreshToken(user);
        verify(this.tokenHasher)
                .hash(refreshToken.getToken());
        verify(this.refreshTokenRepository)
                .save(recordCaptor.capture());

        final LoginAuthenticationToken actualToken = tokenCaptor.getValue();
        assertThat(actualToken.getEmail()).isEqualTo(given.getEmail());
        assertThat(actualToken.getCredentials()).isEqualTo(given.getPassword());
        assertThat(actualToken.getSiteId()).isEqualTo(given.getSiteId());

        final DeviceSession expectedSession = this.getDeviceSession(sessionId,
                user,
                given.getSessionSourceId(),
                SessionStatus.ACTIVE,
                NOW.minusSeconds(3600),
                NOW,
                given.getUserAgent(),
                given.getUserAgent());

        assertThat(sessionCaptor.getValue()).isEqualTo(expectedSession);

        final RefreshTokenRecord expectedRecord = this.getRefreshTokenRecord("hashed",
                user,
                expectedSession,
                RefreshTokenStatus.ACTIVE,
                refreshToken.getExpiresAt(),
                NOW,
                NOW);

        assertThat(recordCaptor.getValue()).isEqualTo(expectedRecord);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenExistingSessionBeyondThrottleInterval_whenExecute_thenUpdateSession() {
        //given
        final UUID siteId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        final LoginRequest given = this.getLoginRequest(siteId);
        final AuthUser user = this.getAuthUser(10L, siteId);
        final AccessToken accessToken = this.getAccessToken("access-token", NOW.plusSeconds(3600));
        final RefreshToken refreshToken = this.getRefreshToken("refresh-token", NOW.plusSeconds(7200));
        final LoginResponse expected = this.getLoginResponse(accessToken.getToken(),
                refreshToken.getToken(),
                3600L,
                "Bearer");
        final DeviceSession existingSession = this.getDeviceSession(sessionId,
                user,
                given.getSessionSourceId(),
                SessionStatus.ACTIVE,
                NOW.minusSeconds(3600),
                NOW.minusSeconds(600),
                given.getUserAgent(),
                given.getUserAgent());

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.authenticationManager.authenticate(any(LoginAuthenticationToken.class)))
                .thenReturn(LoginAuthenticationToken.authenticated(user));
        when(this.deviceSessionRepository.findByUserIdAndSessionSourceId(user.getId(), given.getSessionSourceId()))
                .thenReturn(Optional.of(existingSession));
        when(this.deviceSessionRepository.save(any(DeviceSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(this.tokenProvider.generateAccessToken(user))
                .thenReturn(accessToken);
        when(this.tokenProvider.generateRefreshToken(user))
                .thenReturn(refreshToken);
        when(this.tokenHasher.hash(refreshToken.getToken()))
                .thenReturn("hashed");

        //when
        final LoginResponse actual = this.loginUser.execute(given);

        //then
        final ArgumentCaptor<LoginAuthenticationToken> tokenCaptor = ArgumentCaptor.forClass(LoginAuthenticationToken.class);
        final ArgumentCaptor<RefreshTokenRecord> recordCaptor = ArgumentCaptor.forClass(RefreshTokenRecord.class);
        final ArgumentCaptor<DeviceSession> sessionCaptor = ArgumentCaptor.forClass(DeviceSession.class);

        verify(this.clock)
                .instant();
        verify(this.authenticationManager)
                .authenticate(tokenCaptor.capture());
        verify(this.deviceSessionRepository)
                .findByUserIdAndSessionSourceId(user.getId(), given.getSessionSourceId());
        verify(this.deviceSessionRepository)
                .save(sessionCaptor.capture());
        verify(this.tokenProvider)
                .generateAccessToken(user);
        verify(this.tokenProvider)
                .generateRefreshToken(user);
        verify(this.tokenHasher)
                .hash(refreshToken.getToken());
        verify(this.refreshTokenRepository)
                .save(recordCaptor.capture());

        final LoginAuthenticationToken actualToken = tokenCaptor.getValue();
        assertThat(actualToken.getEmail()).isEqualTo(given.getEmail());
        assertThat(actualToken.getCredentials()).isEqualTo(given.getPassword());
        assertThat(actualToken.getSiteId()).isEqualTo(given.getSiteId());

        final DeviceSession expectedSession = this.getDeviceSession(sessionId,
                user,
                given.getSessionSourceId(),
                SessionStatus.ACTIVE,
                NOW.minusSeconds(3600),
                NOW,
                given.getUserAgent(),
                given.getUserAgent());

        assertThat(sessionCaptor.getValue()).isEqualTo(expectedSession);

        final RefreshTokenRecord expectedRecord = this.getRefreshTokenRecord("hashed",
                user,
                expectedSession,
                RefreshTokenStatus.ACTIVE,
                refreshToken.getExpiresAt(),
                NOW,
                NOW);

        assertThat(recordCaptor.getValue()).isEqualTo(expectedRecord);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenInvalidCredentials_whenExecute_thenThrowInvalidCredentials() {
        //given
        final UUID siteId = UUID.randomUUID();
        final LoginRequest given = this.getLoginRequest(siteId);

        when(this.authenticationManager.authenticate(any(LoginAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.loginUser.execute(given));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(this.authenticationManager)
                .authenticate(any(LoginAuthenticationToken.class));
    }

    @Test
    void givenInactiveUser_whenExecute_thenThrowInvalidCredentials() {
        //given
        final LoginRequest given = this.getLoginRequest(null);
        final ArgumentCaptor<LoginAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(LoginAuthenticationToken.class);

        when(this.authenticationManager.authenticate(any(LoginAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.loginUser.execute(given));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(this.authenticationManager)
                .authenticate(tokenCaptor.capture());
        final LoginAuthenticationToken actualToken = tokenCaptor.getValue();
        assertThat(actualToken.getEmail()).isEqualTo(given.getEmail());
        assertThat(actualToken.getPassword()).isEqualTo(given.getPassword());
        assertThat(actualToken.getSiteId()).isEqualTo(given.getSiteId());
        assertThat(actualToken.getUser()).isNull();
        assertThat(actualToken.isAuthenticated()).isFalse();
    }

    private LoginRequest getLoginRequest(final UUID siteId) {
        return LoginRequest.builder()
                .email("user@sitionix.com")
                .password("StrongPassword123")
                .siteId(siteId)
                .sessionSourceId("device-123")
                .userAgent("Mozilla/5.0")
                .build();
    }

    private AuthUser getAuthUser(final Long userId, final UUID siteId) {
        return AuthUser.builder()
                .id(userId)
                .email("user@sitionix.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .role(UserRole.SITE_USER)
                .siteId(siteId)
                .build();
    }

    private AccessToken getAccessToken(final String token, final Instant expiresAt) {
        return AccessToken.builder()
                .token(token)
                .expiresAt(expiresAt)
                .build();
    }

    private RefreshToken getRefreshToken(final String token, final Instant expiresAt) {
        return RefreshToken.builder()
                .token(token)
                .expiresAt(expiresAt)
                .build();
    }

    private LoginResponse getLoginResponse(final String accessToken,
                                           final String refreshToken,
                                           final Long expiresIn,
                                           final String tokenType) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .tokenType(tokenType)
                .build();
    }

    private RefreshTokenRecord getRefreshTokenRecord(final String tokenHash,
                                                     final AuthUser user,
                                                     final DeviceSession session,
                                                     final RefreshTokenStatus status,
                                                     final Instant expiresAt,
                                                     final Instant createdAt,
                                                     final Instant updatedAt) {
        return RefreshTokenRecord.builder()
                .tokenHash(tokenHash)
                .user(user)
                .session(session)
                .status(status)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    private DeviceSession getDeviceSession(final UUID sessionId,
                                           final AuthUser user,
                                           final String sessionSourceId,
                                           final SessionStatus status,
                                           final Instant createdAt,
                                           final Instant lastUsedAt,
                                           final String initialUserAgent,
                                           final String lastUserAgent) {
        return DeviceSession.builder()
                .id(sessionId)
                .user(user)
                .sessionSourceId(sessionSourceId)
                .status(status)
                .createdAt(createdAt)
                .lastUsedAt(lastUsedAt)
                .initialUserAgent(initialUserAgent)
                .lastUserAgent(lastUserAgent)
                .build();
    }

    private SessionConfig getSessionConfig(final long lastUsedThrottleMinutes) {
        final SessionConfig config = new SessionConfig();
        config.setLastUsedThrottleMinutes(lastUsedThrottleMinutes);
        return config;
    }
}
