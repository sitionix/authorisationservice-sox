package com.sitionix.athssox.application.usecase;

import com.sitionix.athssox.application.config.SessionConfig;
import com.sitionix.athssox.application.service.RefreshTokenRevocationService;
import com.sitionix.athssox.domain.exception.InactiveUserException;
import com.sitionix.athssox.domain.exception.RefreshTokenExpiredException;
import com.sitionix.athssox.domain.exception.RefreshTokenInvalidException;
import com.sitionix.athssox.domain.exception.SessionMismatchException;
import com.sitionix.athssox.domain.exception.SessionNotActiveException;
import com.sitionix.athssox.domain.model.AccessToken;
import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.model.DeviceSession;
import com.sitionix.athssox.domain.model.RefreshAccessTokenRequest;
import com.sitionix.athssox.domain.model.RefreshAccessTokenResponse;
import com.sitionix.athssox.domain.model.RefreshToken;
import com.sitionix.athssox.domain.model.RefreshTokenRecord;
import com.sitionix.athssox.domain.model.RefreshTokenStatus;
import com.sitionix.athssox.domain.model.SessionStatus;
import com.sitionix.athssox.domain.model.TokenType;
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

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshAccessTokenImplTest {

    private static final Instant NOW = Instant.parse("2024-05-01T10:15:30Z");

    private RefreshAccessTokenImpl refreshAccessToken;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenRevocationService refreshTokenRevocationService;

    @Mock
    private DeviceSessionRepository deviceSessionRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private TokenHasher tokenHasher;

    @Mock
    private Clock clock;

    @BeforeEach
    void setUp() {
        final SessionConfig sessionConfig = this.getSessionConfig(5L);
        this.refreshAccessToken = new RefreshAccessTokenImpl(this.refreshTokenRepository,
                this.refreshTokenRevocationService,
                this.deviceSessionRepository,
                this.tokenProvider,
                this.tokenHasher,
                this.clock,
                sessionConfig);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.refreshTokenRepository,
                this.refreshTokenRevocationService,
                this.deviceSessionRepository,
                this.tokenProvider,
                this.tokenHasher,
                this.clock);
    }

    @Test
    void givenValidRequest_whenExecute_thenReturnResponseAndRotateToken() {
        //given
        final UUID siteId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        final String refreshTokenValue = "refresh-token";
        final String newRefreshTokenValue = "new-refresh-token";
        final String sessionSourceId = "session-source-id";
        final String userAgent = "Mozilla/5.0";
        final Instant sessionCreatedAt = NOW.minusSeconds(3600);
        final Instant sessionLastUsedAt = NOW.minusSeconds(600);
        final Instant recordCreatedAt = NOW.minusSeconds(7200);
        final Instant recordUpdatedAt = NOW.minusSeconds(60);
        final Instant tokenExpiresAt = NOW.plusSeconds(3600);
        final Instant newRefreshTokenExpiresAt = NOW.plusSeconds(7200);

        final RefreshAccessTokenRequest given = this.getRefreshAccessTokenRequest(refreshTokenValue,
                sessionSourceId,
                userAgent);
        final AuthUser user = this.getAuthUser(1L, siteId, UserStatus.ACTIVE);
        final DeviceSession session = this.getDeviceSession(sessionId,
                user,
                sessionSourceId,
                SessionStatus.ACTIVE,
                sessionCreatedAt,
                sessionLastUsedAt,
                userAgent,
                "Mozilla/4.0");
        final RefreshTokenRecord tokenRecord = this.getRefreshTokenRecord(10L,
                "hashed-refresh-token",
                user,
                session,
                RefreshTokenStatus.ACTIVE,
                tokenExpiresAt,
                recordCreatedAt,
                recordUpdatedAt,
                null,
                null,
                null);
        final AccessToken accessToken = this.getAccessToken("access-token", NOW.plusSeconds(3600));
        final RefreshToken newRefreshToken = this.getRefreshToken(newRefreshTokenValue, newRefreshTokenExpiresAt);
        final RefreshAccessTokenResponse expected = this.getRefreshAccessTokenResponse("access-token",
                newRefreshTokenValue,
                3600L,
                TokenType.BEARER);

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.tokenHasher.hash(refreshTokenValue))
                .thenReturn("hashed-refresh-token");
        when(this.refreshTokenRepository.findByTokenHash("hashed-refresh-token"))
                .thenReturn(Optional.of(tokenRecord));
        when(this.refreshTokenRepository.revokeIfActive(10L, NOW, "ROTATED"))
                .thenReturn(true);
        when(this.tokenProvider.generateAccessToken(user))
                .thenReturn(accessToken);
        when(this.tokenProvider.generateRefreshToken(user))
                .thenReturn(newRefreshToken);
        when(this.tokenHasher.hash(newRefreshTokenValue))
                .thenReturn("hashed-new-refresh-token");
        when(this.deviceSessionRepository.save(any(DeviceSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        //when
        final RefreshAccessTokenResponse actual = this.refreshAccessToken.execute(given);

        //then
        final ArgumentCaptor<DeviceSession> sessionCaptor = ArgumentCaptor.forClass(DeviceSession.class);
        final ArgumentCaptor<RefreshTokenRecord> recordCaptor = ArgumentCaptor.forClass(RefreshTokenRecord.class);

        verify(this.clock)
                .instant();
        verify(this.tokenHasher)
                .hash(refreshTokenValue);
        verify(this.refreshTokenRepository)
                .findByTokenHash("hashed-refresh-token");
        verify(this.refreshTokenRepository)
                .revokeIfActive(10L, NOW, "ROTATED");
        verify(this.tokenProvider)
                .generateAccessToken(user);
        verify(this.tokenProvider)
                .generateRefreshToken(user);
        verify(this.tokenHasher)
                .hash(newRefreshTokenValue);
        verify(this.refreshTokenRepository)
                .save(recordCaptor.capture());
        verify(this.deviceSessionRepository)
                .save(sessionCaptor.capture());

        final DeviceSession expectedSession = this.getDeviceSession(sessionId,
                user,
                sessionSourceId,
                SessionStatus.ACTIVE,
                sessionCreatedAt,
                NOW,
                userAgent,
                userAgent);

        final RefreshTokenRecord expectedNewRecord = this.getRefreshTokenRecord(null,
                "hashed-new-refresh-token",
                user,
                expectedSession,
                RefreshTokenStatus.ACTIVE,
                newRefreshTokenExpiresAt,
                NOW,
                NOW,
                null,
                null,
                null);
        assertThat(sessionCaptor.getValue()).isEqualTo(expectedSession);
        assertThat(recordCaptor.getAllValues()).isEqualTo(List.of(expectedNewRecord));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenSessionWithinThrottleInterval_whenExecute_thenSkipSessionUpdate() {
        //given
        final UUID siteId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        final String refreshTokenValue = "refresh-token";
        final String newRefreshTokenValue = "new-refresh-token";
        final String sessionSourceId = "session-source-id";
        final String userAgent = "Mozilla/5.0";
        final Instant sessionCreatedAt = NOW.minusSeconds(3600);
        final Instant sessionLastUsedAt = NOW.minusSeconds(60);
        final Instant recordCreatedAt = NOW.minusSeconds(7200);
        final Instant recordUpdatedAt = NOW.minusSeconds(60);
        final Instant tokenExpiresAt = NOW.plusSeconds(3600);
        final Instant newRefreshTokenExpiresAt = NOW.plusSeconds(7200);

        final RefreshAccessTokenRequest given = this.getRefreshAccessTokenRequest(refreshTokenValue,
                sessionSourceId,
                userAgent);
        final AuthUser user = this.getAuthUser(1L, siteId, UserStatus.ACTIVE);
        final DeviceSession session = this.getDeviceSession(sessionId,
                user,
                sessionSourceId,
                SessionStatus.ACTIVE,
                sessionCreatedAt,
                sessionLastUsedAt,
                userAgent,
                userAgent);
        final RefreshTokenRecord tokenRecord = this.getRefreshTokenRecord(10L,
                "hashed-refresh-token",
                user,
                session,
                RefreshTokenStatus.ACTIVE,
                tokenExpiresAt,
                recordCreatedAt,
                recordUpdatedAt,
                null,
                null,
                null);
        final AccessToken accessToken = this.getAccessToken("access-token", NOW.plusSeconds(3600));
        final RefreshToken newRefreshToken = this.getRefreshToken(newRefreshTokenValue, newRefreshTokenExpiresAt);
        final RefreshAccessTokenResponse expected = this.getRefreshAccessTokenResponse("access-token",
                newRefreshTokenValue,
                3600L,
                TokenType.BEARER);

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.tokenHasher.hash(refreshTokenValue))
                .thenReturn("hashed-refresh-token");
        when(this.refreshTokenRepository.findByTokenHash("hashed-refresh-token"))
                .thenReturn(Optional.of(tokenRecord));
        when(this.refreshTokenRepository.revokeIfActive(10L, NOW, "ROTATED"))
                .thenReturn(true);
        when(this.tokenProvider.generateAccessToken(user))
                .thenReturn(accessToken);
        when(this.tokenProvider.generateRefreshToken(user))
                .thenReturn(newRefreshToken);
        when(this.tokenHasher.hash(newRefreshTokenValue))
                .thenReturn("hashed-new-refresh-token");

        //when
        final RefreshAccessTokenResponse actual = this.refreshAccessToken.execute(given);

        //then
        final ArgumentCaptor<RefreshTokenRecord> recordCaptor = ArgumentCaptor.forClass(RefreshTokenRecord.class);

        verify(this.clock)
                .instant();
        verify(this.tokenHasher)
                .hash(refreshTokenValue);
        verify(this.refreshTokenRepository)
                .findByTokenHash("hashed-refresh-token");
        verify(this.refreshTokenRepository)
                .revokeIfActive(10L, NOW, "ROTATED");
        verify(this.tokenProvider)
                .generateAccessToken(user);
        verify(this.tokenProvider)
                .generateRefreshToken(user);
        verify(this.tokenHasher)
                .hash(newRefreshTokenValue);
        verify(this.refreshTokenRepository)
                .save(recordCaptor.capture());
        verify(this.deviceSessionRepository, times(0))
                .save(any(DeviceSession.class));

        final DeviceSession expectedSession = this.getDeviceSession(sessionId,
                user,
                sessionSourceId,
                SessionStatus.ACTIVE,
                sessionCreatedAt,
                sessionLastUsedAt,
                userAgent,
                userAgent);

        final RefreshTokenRecord expectedNewRecord = this.getRefreshTokenRecord(null,
                "hashed-new-refresh-token",
                user,
                expectedSession,
                RefreshTokenStatus.ACTIVE,
                newRefreshTokenExpiresAt,
                NOW,
                NOW,
                null,
                null,
                null);
        assertThat(recordCaptor.getAllValues()).isEqualTo(List.of(expectedNewRecord));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenMissingToken_whenExecute_thenThrowRefreshTokenInvalidException() {
        //given
        final String refreshTokenValue = "refresh-token";
        final String sessionSourceId = "session-source-id";
        final String userAgent = "Mozilla/5.0";

        final RefreshAccessTokenRequest given = this.getRefreshAccessTokenRequest(refreshTokenValue,
                sessionSourceId,
                userAgent);

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.tokenHasher.hash(refreshTokenValue))
                .thenReturn("hashed-refresh-token");
        when(this.refreshTokenRepository.findByTokenHash("hashed-refresh-token"))
                .thenReturn(Optional.empty());

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.refreshAccessToken.execute(given));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(RefreshTokenInvalidException.class)
                .hasMessage("Refresh token is invalid or revoked");
        verify(this.clock)
                .instant();
        verify(this.tokenHasher)
                .hash(refreshTokenValue);
        verify(this.refreshTokenRepository)
                .findByTokenHash("hashed-refresh-token");
    }

    @Test
    void givenExpiredToken_whenExecute_thenThrowRefreshTokenExpiredException() {
        //given
        final UUID siteId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        final String refreshTokenValue = "refresh-token";
        final String sessionSourceId = "session-source-id";
        final String userAgent = "Mozilla/5.0";

        final RefreshAccessTokenRequest given = this.getRefreshAccessTokenRequest(refreshTokenValue,
                sessionSourceId,
                userAgent);
        final AuthUser user = this.getAuthUser(2L, siteId, UserStatus.ACTIVE);
        final DeviceSession session = this.getDeviceSession(sessionId,
                user,
                sessionSourceId,
                SessionStatus.ACTIVE,
                NOW.minusSeconds(3600),
                NOW.minusSeconds(600),
                userAgent,
                userAgent);
        final RefreshTokenRecord tokenRecord = this.getRefreshTokenRecord(11L,
                "hashed-refresh-token",
                user,
                session,
                RefreshTokenStatus.ACTIVE,
                NOW,
                NOW.minusSeconds(7200),
                NOW.minusSeconds(300),
                null,
                null,
                null);

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.tokenHasher.hash(refreshTokenValue))
                .thenReturn("hashed-refresh-token");
        when(this.refreshTokenRepository.findByTokenHash("hashed-refresh-token"))
                .thenReturn(Optional.of(tokenRecord));

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.refreshAccessToken.execute(given));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(RefreshTokenExpiredException.class)
                .hasMessage("Refresh token has expired");
        verify(this.clock)
                .instant();
        verify(this.tokenHasher)
                .hash(refreshTokenValue);
        verify(this.refreshTokenRepository)
                .findByTokenHash("hashed-refresh-token");
    }

    @Test
    void givenNullExpiry_whenExecute_thenThrowRefreshTokenExpiredException() {
        //given
        final UUID siteId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        final String refreshTokenValue = "refresh-token";
        final String sessionSourceId = "session-source-id";
        final String userAgent = "Mozilla/5.0";
        final Instant sessionCreatedAt = NOW.minusSeconds(3600);
        final Instant sessionLastUsedAt = NOW.minusSeconds(600);
        final Instant recordCreatedAt = NOW.minusSeconds(7200);
        final Instant recordUpdatedAt = NOW.minusSeconds(300);

        final RefreshAccessTokenRequest given = this.getRefreshAccessTokenRequest(refreshTokenValue,
                sessionSourceId,
                userAgent);
        final AuthUser user = this.getAuthUser(2L, siteId, UserStatus.ACTIVE);
        final DeviceSession session = this.getDeviceSession(sessionId,
                user,
                sessionSourceId,
                SessionStatus.ACTIVE,
                sessionCreatedAt,
                sessionLastUsedAt,
                userAgent,
                userAgent);
        final RefreshTokenRecord tokenRecord = this.getRefreshTokenRecord(11L,
                "hashed-refresh-token",
                user,
                session,
                RefreshTokenStatus.ACTIVE,
                null,
                recordCreatedAt,
                recordUpdatedAt,
                null,
                null,
                null);

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.tokenHasher.hash(refreshTokenValue))
                .thenReturn("hashed-refresh-token");
        when(this.refreshTokenRepository.findByTokenHash("hashed-refresh-token"))
                .thenReturn(Optional.of(tokenRecord));

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.refreshAccessToken.execute(given));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(RefreshTokenExpiredException.class)
                .hasMessage("Refresh token has expired");
        verify(this.clock)
                .instant();
        verify(this.tokenHasher)
                .hash(refreshTokenValue);
        verify(this.refreshTokenRepository)
                .findByTokenHash("hashed-refresh-token");
    }

    @Test
    void givenMissingSessionId_whenExecute_thenThrowSessionNotActiveException() {
        //given
        final UUID siteId = UUID.randomUUID();
        final String refreshTokenValue = "refresh-token";
        final String sessionSourceId = "session-source-id";
        final String userAgent = "Mozilla/5.0";
        final Instant sessionCreatedAt = NOW.minusSeconds(3600);
        final Instant sessionLastUsedAt = NOW.minusSeconds(600);
        final Instant recordCreatedAt = NOW.minusSeconds(7200);
        final Instant recordUpdatedAt = NOW.minusSeconds(300);
        final Instant tokenExpiresAt = NOW.plusSeconds(3600);

        final RefreshAccessTokenRequest given = this.getRefreshAccessTokenRequest(refreshTokenValue,
                sessionSourceId,
                userAgent);
        final AuthUser user = this.getAuthUser(3L, siteId, UserStatus.ACTIVE);
        final DeviceSession session = this.getDeviceSession(null,
                user,
                sessionSourceId,
                SessionStatus.ACTIVE,
                sessionCreatedAt,
                sessionLastUsedAt,
                userAgent,
                userAgent);
        final RefreshTokenRecord tokenRecord = this.getRefreshTokenRecord(12L,
                "hashed-refresh-token",
                user,
                session,
                RefreshTokenStatus.ACTIVE,
                tokenExpiresAt,
                recordCreatedAt,
                recordUpdatedAt,
                null,
                null,
                null);

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.tokenHasher.hash(refreshTokenValue))
                .thenReturn("hashed-refresh-token");
        when(this.refreshTokenRepository.findByTokenHash("hashed-refresh-token"))
                .thenReturn(Optional.of(tokenRecord));

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.refreshAccessToken.execute(given));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(SessionNotActiveException.class)
                .hasMessage("Session is not active or does not exist");
        verify(this.clock)
                .instant();
        verify(this.tokenHasher)
                .hash(refreshTokenValue);
        verify(this.refreshTokenRepository)
                .findByTokenHash("hashed-refresh-token");
    }

    @Test
    void givenSessionSourceMismatch_whenExecute_thenMarkSessionSuspiciousAndThrowSessionMismatchException() {
        //given
        final UUID siteId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        final String refreshTokenValue = "refresh-token";
        final String sessionSourceId = "session-source-id";
        final String requestUserAgent = "Mozilla/5.0";

        final RefreshAccessTokenRequest given = this.getRefreshAccessTokenRequest(refreshTokenValue,
                sessionSourceId,
                requestUserAgent);
        final AuthUser user = this.getAuthUser(3L, siteId, UserStatus.ACTIVE);
        final DeviceSession session = this.getDeviceSession(sessionId,
                user,
                "other-session-source-id",
                SessionStatus.ACTIVE,
                NOW.minusSeconds(3600),
                NOW.minusSeconds(600),
                "Mozilla/4.0",
                "Mozilla/4.0");
        final RefreshTokenRecord tokenRecord = this.getRefreshTokenRecord(12L,
                "hashed-refresh-token",
                user,
                session,
                RefreshTokenStatus.ACTIVE,
                NOW.plusSeconds(3600),
                NOW.minusSeconds(7200),
                NOW.minusSeconds(300),
                null,
                null,
                null);

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.tokenHasher.hash(refreshTokenValue))
                .thenReturn("hashed-refresh-token");
        when(this.refreshTokenRepository.findByTokenHash("hashed-refresh-token"))
                .thenReturn(Optional.of(tokenRecord));

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.refreshAccessToken.execute(given));

        //then
        final ArgumentCaptor<DeviceSession> sessionCaptor = ArgumentCaptor.forClass(DeviceSession.class);

        assertThat(actualThrowable)
                .isInstanceOf(SessionMismatchException.class)
                .hasMessage("Session does not match original token context");
        verify(this.clock)
                .instant();
        verify(this.tokenHasher)
                .hash(refreshTokenValue);
        verify(this.refreshTokenRepository)
                .findByTokenHash("hashed-refresh-token");
        verify(this.refreshTokenRevocationService)
                .revokeIfActive(12L, NOW, "MISMATCH");
        verify(this.deviceSessionRepository)
                .save(sessionCaptor.capture());

        final DeviceSession expectedSession = this.getDeviceSession(sessionId,
                user,
                "other-session-source-id",
                SessionStatus.SUSPICIOUS,
                NOW.minusSeconds(3600),
                NOW,
                "Mozilla/4.0",
                requestUserAgent);

        assertThat(sessionCaptor.getValue()).isEqualTo(expectedSession);
    }

    @Test
    void givenReplayedToken_whenExecute_thenMarkSessionSuspiciousAndThrowRefreshTokenInvalidException() {
        //given
        final UUID siteId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        final String refreshTokenValue = "refresh-token";
        final String sessionSourceId = "session-source-id";
        final String requestUserAgent = "Mozilla/5.0";

        final RefreshAccessTokenRequest given = this.getRefreshAccessTokenRequest(refreshTokenValue,
                sessionSourceId,
                requestUserAgent);
        final AuthUser user = this.getAuthUser(4L, siteId, UserStatus.ACTIVE);
        final DeviceSession session = this.getDeviceSession(sessionId,
                user,
                sessionSourceId,
                SessionStatus.ACTIVE,
                NOW.minusSeconds(3600),
                NOW.minusSeconds(600),
                requestUserAgent,
                requestUserAgent);
        final RefreshTokenRecord tokenRecord = this.getRefreshTokenRecord(13L,
                "hashed-refresh-token",
                user,
                session,
                RefreshTokenStatus.ACTIVE,
                NOW.plusSeconds(3600),
                NOW.minusSeconds(7200),
                NOW.minusSeconds(300),
                NOW.minusSeconds(10),
                null,
                null);

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.tokenHasher.hash(refreshTokenValue))
                .thenReturn("hashed-refresh-token");
        when(this.refreshTokenRepository.findByTokenHash("hashed-refresh-token"))
                .thenReturn(Optional.of(tokenRecord));

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.refreshAccessToken.execute(given));

        //then
        final ArgumentCaptor<DeviceSession> sessionCaptor = ArgumentCaptor.forClass(DeviceSession.class);

        assertThat(actualThrowable)
                .isInstanceOf(RefreshTokenInvalidException.class)
                .hasMessage("Refresh token is invalid or revoked");
        verify(this.clock)
                .instant();
        verify(this.tokenHasher)
                .hash(refreshTokenValue);
        verify(this.refreshTokenRepository)
                .findByTokenHash("hashed-refresh-token");
        verify(this.refreshTokenRevocationService)
                .revokeIfActive(13L, NOW, "REPLAY");
        verify(this.deviceSessionRepository)
                .save(sessionCaptor.capture());

        final DeviceSession expectedSession = this.getDeviceSession(sessionId,
                user,
                sessionSourceId,
                SessionStatus.SUSPICIOUS,
                NOW.minusSeconds(3600),
                NOW,
                requestUserAgent,
                requestUserAgent);

        assertThat(sessionCaptor.getValue()).isEqualTo(expectedSession);
    }

    @Test
    void givenInactiveSession_whenExecute_thenThrowSessionNotActiveException() {
        //given
        final UUID siteId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        final String refreshTokenValue = "refresh-token";
        final String sessionSourceId = "session-source-id";
        final String requestUserAgent = "Mozilla/5.0";

        final RefreshAccessTokenRequest given = this.getRefreshAccessTokenRequest(refreshTokenValue,
                sessionSourceId,
                requestUserAgent);
        final AuthUser user = this.getAuthUser(5L, siteId, UserStatus.ACTIVE);
        final DeviceSession session = this.getDeviceSession(sessionId,
                user,
                sessionSourceId,
                SessionStatus.REVOKED_BY_USER,
                NOW.minusSeconds(3600),
                NOW.minusSeconds(600),
                requestUserAgent,
                requestUserAgent);
        final RefreshTokenRecord tokenRecord = this.getRefreshTokenRecord(14L,
                "hashed-refresh-token",
                user,
                session,
                RefreshTokenStatus.ACTIVE,
                NOW.plusSeconds(3600),
                NOW.minusSeconds(7200),
                NOW.minusSeconds(300),
                null,
                null,
                null);

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.tokenHasher.hash(refreshTokenValue))
                .thenReturn("hashed-refresh-token");
        when(this.refreshTokenRepository.findByTokenHash("hashed-refresh-token"))
                .thenReturn(Optional.of(tokenRecord));

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.refreshAccessToken.execute(given));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(SessionNotActiveException.class)
                .hasMessage("Session is not active or does not exist");
        verify(this.clock)
                .instant();
        verify(this.tokenHasher)
                .hash(refreshTokenValue);
        verify(this.refreshTokenRepository)
                .findByTokenHash("hashed-refresh-token");
        verify(this.refreshTokenRevocationService)
                .revokeIfActive(14L, NOW, "SUSPICIOUS");
    }

    @Test
    void givenInactiveUser_whenExecute_thenThrowInactiveUserException() {
        //given
        final UUID siteId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        final String refreshTokenValue = "refresh-token";
        final String sessionSourceId = "session-source-id";
        final String userAgent = "Mozilla/5.0";
        final Instant sessionCreatedAt = NOW.minusSeconds(3600);
        final Instant sessionLastUsedAt = NOW.minusSeconds(600);
        final Instant recordCreatedAt = NOW.minusSeconds(7200);
        final Instant recordUpdatedAt = NOW.minusSeconds(300);
        final Instant tokenExpiresAt = NOW.plusSeconds(3600);

        final RefreshAccessTokenRequest given = this.getRefreshAccessTokenRequest(refreshTokenValue,
                sessionSourceId,
                userAgent);
        final AuthUser user = this.getAuthUser(6L, siteId, UserStatus.INACTIVE);
        final DeviceSession session = this.getDeviceSession(sessionId,
                user,
                sessionSourceId,
                SessionStatus.ACTIVE,
                sessionCreatedAt,
                sessionLastUsedAt,
                userAgent,
                userAgent);
        final RefreshTokenRecord tokenRecord = this.getRefreshTokenRecord(15L,
                "hashed-refresh-token",
                user,
                session,
                RefreshTokenStatus.ACTIVE,
                tokenExpiresAt,
                recordCreatedAt,
                recordUpdatedAt,
                null,
                null,
                null);

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.tokenHasher.hash(refreshTokenValue))
                .thenReturn("hashed-refresh-token");
        when(this.refreshTokenRepository.findByTokenHash("hashed-refresh-token"))
                .thenReturn(Optional.of(tokenRecord));

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.refreshAccessToken.execute(given));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(InactiveUserException.class)
                .hasMessage("Account is not yet activated");
        verify(this.clock)
                .instant();
        verify(this.tokenHasher)
                .hash(refreshTokenValue);
        verify(this.refreshTokenRepository)
                .findByTokenHash("hashed-refresh-token");
    }

    @Test
    void givenFailedRevoke_whenExecute_thenMarkSessionSuspiciousAndThrowRefreshTokenInvalidException() {
        //given
        final UUID siteId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        final String refreshTokenValue = "refresh-token";
        final String sessionSourceId = "session-source-id";
        final String requestUserAgent = "Mozilla/5.0";
        final Instant sessionCreatedAt = NOW.minusSeconds(3600);
        final Instant sessionLastUsedAt = NOW.minusSeconds(60);
        final Instant recordCreatedAt = NOW.minusSeconds(7200);
        final Instant recordUpdatedAt = NOW.minusSeconds(300);
        final Instant tokenExpiresAt = NOW.plusSeconds(3600);

        final RefreshAccessTokenRequest given = this.getRefreshAccessTokenRequest(refreshTokenValue,
                sessionSourceId,
                requestUserAgent);
        final AuthUser user = this.getAuthUser(7L, siteId, UserStatus.ACTIVE);
        final DeviceSession session = this.getDeviceSession(sessionId,
                user,
                sessionSourceId,
                SessionStatus.ACTIVE,
                sessionCreatedAt,
                sessionLastUsedAt,
                "Mozilla/4.0",
                "Mozilla/4.0");
        final RefreshTokenRecord tokenRecord = this.getRefreshTokenRecord(16L,
                "hashed-refresh-token",
                user,
                session,
                RefreshTokenStatus.ACTIVE,
                tokenExpiresAt,
                recordCreatedAt,
                recordUpdatedAt,
                null,
                null,
                null);

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.tokenHasher.hash(refreshTokenValue))
                .thenReturn("hashed-refresh-token");
        when(this.refreshTokenRepository.findByTokenHash("hashed-refresh-token"))
                .thenReturn(Optional.of(tokenRecord));
        when(this.refreshTokenRepository.revokeIfActive(16L, NOW, "ROTATED"))
                .thenReturn(false);

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.refreshAccessToken.execute(given));

        //then
        final ArgumentCaptor<DeviceSession> sessionCaptor = ArgumentCaptor.forClass(DeviceSession.class);

        assertThat(actualThrowable)
                .isInstanceOf(RefreshTokenInvalidException.class)
                .hasMessage("Refresh token is invalid or revoked");
        verify(this.clock)
                .instant();
        verify(this.tokenHasher)
                .hash(refreshTokenValue);
        verify(this.refreshTokenRepository)
                .findByTokenHash("hashed-refresh-token");
        verify(this.refreshTokenRepository)
                .revokeIfActive(16L, NOW, "ROTATED");
        verify(this.deviceSessionRepository)
                .save(sessionCaptor.capture());

        final DeviceSession expectedSession = this.getDeviceSession(sessionId,
                user,
                sessionSourceId,
                SessionStatus.SUSPICIOUS,
                sessionCreatedAt,
                sessionLastUsedAt,
                "Mozilla/4.0",
                requestUserAgent);

        assertThat(sessionCaptor.getValue()).isEqualTo(expectedSession);
    }

    private RefreshAccessTokenRequest getRefreshAccessTokenRequest(final String refreshToken,
                                                                   final String sessionSourceId,
                                                                   final String userAgent) {
        return RefreshAccessTokenRequest.builder()
                .refreshToken(refreshToken)
                .sessionSourceId(sessionSourceId)
                .userAgent(userAgent)
                .build();
    }

    private AuthUser getAuthUser(final Long userId, final UUID siteId, final UserStatus status) {
        return AuthUser.builder()
                .id(userId)
                .email("user@sitionix.com")
                .passwordHash("hashed")
                .status(status)
                .role(UserRole.SITE_USER)
                .siteId(siteId)
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

    private RefreshTokenRecord getRefreshTokenRecord(final Long id,
                                                     final String tokenHash,
                                                     final AuthUser user,
                                                     final DeviceSession session,
                                                     final RefreshTokenStatus status,
                                                     final Instant expiresAt,
                                                     final Instant createdAt,
                                                     final Instant updatedAt,
                                                     final Instant usedAt,
                                                     final Instant revokedAt,
                                                     final String revokedReason) {
        return RefreshTokenRecord.builder()
                .id(id)
                .tokenHash(tokenHash)
                .user(user)
                .session(session)
                .status(status)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .usedAt(usedAt)
                .revokedAt(revokedAt)
                .revokedReason(revokedReason)
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

    private RefreshAccessTokenResponse getRefreshAccessTokenResponse(final String accessToken,
                                                                     final String refreshToken,
                                                                     final long expiresIn,
                                                                     final TokenType tokenType) {
        return RefreshAccessTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .tokenType(tokenType)
                .build();
    }

    private SessionConfig getSessionConfig(final long lastUsedThrottleMinutes) {
        final SessionConfig config = new SessionConfig();
        config.setLastUsedThrottleMinutes(lastUsedThrottleMinutes);
        return config;
    }
}
