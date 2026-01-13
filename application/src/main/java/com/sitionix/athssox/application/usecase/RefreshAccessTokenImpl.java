package com.sitionix.athssox.application.usecase;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshAccessTokenImpl implements RefreshAccessToken {

    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceSessionRepository deviceSessionRepository;
    private final TokenProvider tokenProvider;
    private final TokenHasher tokenHasher;
    private final Clock clock;

    @Override
    @Transactional
    public RefreshAccessTokenResponse execute(final RefreshAccessTokenRequest refreshAccessTokenRequest) {
        final Instant now = this.clock.instant();
        final RefreshTokenRecord tokenRecord = this.findValidToken(refreshAccessTokenRequest, now);
        final DeviceSession session = this.validateSession(tokenRecord, refreshAccessTokenRequest);
        final AuthUser user = tokenRecord.getUser();
        final AccessToken accessToken = this.tokenProvider.generateAccessToken(user);
        final RefreshToken newRefreshToken = this.tokenProvider.generateRefreshToken(user);

        this.rotateRefreshToken(tokenRecord, user, session, newRefreshToken, now);
        this.updateSession(session, refreshAccessTokenRequest, now);

        final long expiresIn = Duration.between(now, accessToken.getExpiresAt()).getSeconds();
        return RefreshAccessTokenResponse.builder()
                .accessToken(accessToken.getToken())
                .refreshToken(newRefreshToken.getToken())
                .expiresIn(expiresIn)
                .tokenType("Bearer")
                .build();
    }

    private RefreshTokenRecord findValidToken(final RefreshAccessTokenRequest request, final Instant now) {
        final String hashedToken = this.tokenHasher.hash(request.getRefreshToken());
        final RefreshTokenRecord tokenRecord = this.refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new RefreshTokenInvalidException("Refresh token is invalid or revoked"));

        if (isExpired(tokenRecord, now)) {
            throw new RefreshTokenExpiredException("Refresh token has expired");
        }

        if (isRevoked(tokenRecord)) {
            this.markSessionSuspicious(tokenRecord, request, now);
            throw new RefreshTokenInvalidException("Refresh token is invalid or revoked");
        }

        return tokenRecord;
    }

    private DeviceSession validateSession(final RefreshTokenRecord tokenRecord,
                                          final RefreshAccessTokenRequest request) {
        final DeviceSession session = this.deviceSessionRepository
                .findByUserIdAndSessionSourceId(tokenRecord.getUser().getId(), request.getSessionSourceId())
                .orElseThrow(() -> new SessionNotActiveException("Session is not active or does not exist"));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new SessionNotActiveException("Session is not active or does not exist");
        }

        if (tokenRecord.getSession() == null || tokenRecord.getSession().getId() == null) {
            throw new SessionMismatchException("Session does not match original token context");
        }

        if (!tokenRecord.getSession().getId().equals(session.getId())) {
            throw new SessionMismatchException("Session does not match original token context");
        }

        return session;
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
        session.setLastUsedAt(now);
        session.setLastUserAgent(request.getUserAgent());
        this.deviceSessionRepository.save(session);
    }

    private void markSessionSuspicious(final RefreshTokenRecord tokenRecord,
                                       final RefreshAccessTokenRequest request,
                                       final Instant now) {
        if (tokenRecord.getSession() == null) {
            return;
        }
        final DeviceSession session = tokenRecord.getSession();
        if (session.getStatus() == SessionStatus.SUSPICIOUS) {
            return;
        }
        session.setStatus(SessionStatus.SUSPICIOUS);
        session.setLastUsedAt(now);
        session.setLastUserAgent(request.getUserAgent());
        this.deviceSessionRepository.save(session);
    }

    private boolean isExpired(final RefreshTokenRecord tokenRecord, final Instant now) {
        return tokenRecord.getExpiresAt() == null || !tokenRecord.getExpiresAt().isAfter(now);
    }

    private boolean isRevoked(final RefreshTokenRecord tokenRecord) {
        if (tokenRecord.getRevokedAt() != null) {
            return true;
        }
        if (tokenRecord.getStatus() == null) {
            return false;
        }
        return tokenRecord.getStatus() == RefreshTokenStatus.REVOKED;
    }
}
