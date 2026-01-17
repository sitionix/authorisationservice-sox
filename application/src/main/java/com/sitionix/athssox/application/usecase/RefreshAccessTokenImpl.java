package com.sitionix.athssox.application.usecase;

import com.sitionix.athssox.application.config.SessionConfig;
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
import com.sitionix.athssox.domain.model.UserStatus;
import com.sitionix.athssox.domain.repository.DeviceSessionRepository;
import com.sitionix.athssox.domain.repository.RefreshTokenRepository;
import com.sitionix.athssox.domain.service.TokenHasher;
import com.sitionix.athssox.domain.service.TokenProvider;
import com.sitionix.athssox.domain.usecase.RefreshAccessToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshAccessTokenImpl implements RefreshAccessToken {

    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceSessionRepository deviceSessionRepository;
    private final TokenProvider tokenProvider;
    private final TokenHasher tokenHasher;
    private final Clock clock;
    private final SessionConfig sessionConfig;

    @Override
    @Transactional(noRollbackFor = {RefreshTokenInvalidException.class, SessionMismatchException.class})
    public RefreshAccessTokenResponse execute(final RefreshAccessTokenRequest refreshAccessTokenRequest) {
        final Instant now = this.clock.instant();
        final RefreshTokenRecord tokenRecord = this.findTokenRecord(refreshAccessTokenRequest, now);
        final DeviceSession session = this.getTokenSession(tokenRecord);

        this.ensureSessionActive(session);
        this.ensureSessionSourceMatches(session, refreshAccessTokenRequest, now);
        this.ensureTokenNotReplayed(tokenRecord, session, refreshAccessTokenRequest, now);
        this.detectSessionAnomaly(session, refreshAccessTokenRequest);

        final AuthUser user = tokenRecord.getUser();
        this.ensureUserActive(user);
        final AccessToken accessToken = this.tokenProvider.generateAccessToken(user);
        final RefreshToken newRefreshToken = this.tokenProvider.generateRefreshToken(user);

        this.rotateRefreshToken(tokenRecord, user, session, newRefreshToken, now);
        this.updateSession(session, refreshAccessTokenRequest, now);

        final long expiresIn = Duration.between(now, accessToken.getExpiresAt()).getSeconds();
        return RefreshAccessTokenResponse.builder()
                .accessToken(accessToken.getToken())
                .refreshToken(newRefreshToken.getToken())
                .expiresIn(expiresIn)
                .tokenType(TokenType.BEARER)
                .build();
    }

    private RefreshTokenRecord findTokenRecord(final RefreshAccessTokenRequest request, final Instant now) {
        final String hashedToken = this.tokenHasher.hash(request.getRefreshToken());
        final RefreshTokenRecord tokenRecord = this.refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new RefreshTokenInvalidException("Refresh token is invalid or revoked"));

        if (isExpired(tokenRecord, now)) {
            throw new RefreshTokenExpiredException("Refresh token has expired");
        }

        return tokenRecord;
    }

    private DeviceSession getTokenSession(final RefreshTokenRecord tokenRecord) {
        final DeviceSession session = tokenRecord.getSession();
        if (session == null || session.getId() == null) {
            throw new SessionNotActiveException("Session is not active or does not exist");
        }
        return session;
    }

    private void ensureSessionActive(final DeviceSession session) {
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new SessionNotActiveException("Session is not active or does not exist");
        }
    }

    private void ensureSessionSourceMatches(final DeviceSession session,
                                            final RefreshAccessTokenRequest request,
                                            final Instant now) {
        if (!Objects.equals(session.getSessionSourceId(), request.getSessionSourceId())) {
            // TODO: emit session_context_mismatch_detected
            this.markSessionSuspicious(session, request, now);
            throw new SessionMismatchException("Session does not match original token context");
        }
    }

    private void ensureTokenNotReplayed(final RefreshTokenRecord tokenRecord,
                                        final DeviceSession session,
                                        final RefreshAccessTokenRequest request,
                                        final Instant now) {
        if (isReplayed(tokenRecord)) {
            // TODO: emit replay_attack_detected
            this.markSessionSuspicious(session, request, now);
            throw new RefreshTokenInvalidException("Refresh token is invalid or revoked");
        }
    }

    private void ensureUserActive(final AuthUser user) {
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            throw new InactiveUserException("Account is not yet activated");
        }
    }

    private void detectSessionAnomaly(final DeviceSession session,
                                      final RefreshAccessTokenRequest request) {
        if (isUserAgentChanged(session, request.getUserAgent())) {
            // TODO: emit audit event session_user_agent_changed
        }
    }

    private void rotateRefreshToken(final RefreshTokenRecord tokenRecord,
                                    final AuthUser user,
                                    final DeviceSession session,
                                    final RefreshToken newRefreshToken,
                                    final Instant now) {
        this.refreshTokenRepository.save(RefreshTokenRecord.builder()
                .tokenHash(this.tokenHasher.hash(newRefreshToken.getToken()))
                .user(user)
                .session(session)
                .status(RefreshTokenStatus.ACTIVE)
                .expiresAt(newRefreshToken.getExpiresAt())
                .createdAt(now)
                .updatedAt(now)
                .build());

        tokenRecord.setStatus(RefreshTokenStatus.REVOKED);
        tokenRecord.setUsedAt(now);
        tokenRecord.setRevokedAt(now);
        tokenRecord.setRevokedReason("ROTATED");
        tokenRecord.setUpdatedAt(now);
        this.refreshTokenRepository.save(tokenRecord);
    }

    private void updateSession(final DeviceSession session,
                               final RefreshAccessTokenRequest request,
                               final Instant now) {
        final boolean shouldUpdateLastUsedAt = this.shouldUpdateLastUsedAt(session, now);
        final boolean shouldUpdateUserAgent = !Objects.equals(session.getLastUserAgent(), request.getUserAgent());

        if (!shouldUpdateLastUsedAt && !shouldUpdateUserAgent) {
            return;
        }

        if (shouldUpdateLastUsedAt) {
            session.setLastUsedAt(now);
        }
        if (shouldUpdateUserAgent) {
            session.setLastUserAgent(request.getUserAgent());
        }
        this.deviceSessionRepository.save(session);
    }

    private void markSessionSuspicious(final DeviceSession session,
                                       final RefreshAccessTokenRequest request,
                                       final Instant now) {
        if (session == null || session.getStatus() != SessionStatus.ACTIVE) {
            return;
        }
        session.setStatus(SessionStatus.SUSPICIOUS);
        if (this.shouldUpdateLastUsedAt(session, now)) {
            session.setLastUsedAt(now);
        }
        if (!Objects.equals(session.getLastUserAgent(), request.getUserAgent())) {
            session.setLastUserAgent(request.getUserAgent());
        }
        this.deviceSessionRepository.save(session);
    }

    private boolean isExpired(final RefreshTokenRecord tokenRecord, final Instant now) {
        return tokenRecord.getExpiresAt() == null || !tokenRecord.getExpiresAt().isAfter(now);
    }

    private boolean isReplayed(final RefreshTokenRecord tokenRecord) {
        if (tokenRecord.getUsedAt() != null) {
            return true;
        }
        if (tokenRecord.getRevokedAt() != null) {
            return true;
        }
        return tokenRecord.getStatus() == RefreshTokenStatus.REVOKED;
    }

    private boolean isUserAgentChanged(final DeviceSession session, final String userAgent) {
        if (userAgent == null || session.getLastUserAgent() == null) {
            return false;
        }
        return !session.getLastUserAgent().equals(userAgent);
    }

    private boolean shouldUpdateLastUsedAt(final DeviceSession session, final Instant now) {
        if (session.getLastUsedAt() == null) {
            return true;
        }
        final Duration throttleInterval = this.sessionConfig.getLastUsedThrottleInterval();
        if (throttleInterval.isZero() || throttleInterval.isNegative()) {
            return true;
        }
        final Instant threshold = now.minus(throttleInterval);
        return !session.getLastUsedAt().isAfter(threshold);
    }
}
