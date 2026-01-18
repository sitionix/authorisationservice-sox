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
import com.sitionix.athssox.domain.repository.DeviceSessionRepository;
import com.sitionix.athssox.domain.repository.RefreshTokenRepository;
import com.sitionix.athssox.domain.service.TokenHasher;
import com.sitionix.athssox.domain.service.TokenProvider;
import com.sitionix.athssox.domain.usecase.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginUserImpl implements LoginUser {

    private static final String LOGIN_REVOKE_REASON = "LOGIN";

    private final DeviceSessionRepository deviceSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;
    private final TokenHasher tokenHasher;
    private final AuthenticationManager authenticationManager;
    private final Clock clock;
    private final SessionConfig sessionConfig;

    @Override
    @Transactional
    public LoginResponse execute(@Valid final LoginRequest loginRequest) {
        final Authentication authentication = this.authenticationManager.authenticate(
                LoginAuthenticationToken.unauthenticated(loginRequest.getEmail(),
                        loginRequest.getPassword(),
                        loginRequest.getSiteId()));

        final AuthUser user = ((LoginAuthenticationToken) authentication).getUser();
        final Instant now = this.clock.instant();
        final DeviceSession session = this.resolveSession(user, loginRequest, now);
        this.revokeActiveTokensForSession(session, now);
        final AccessToken accessToken = this.tokenProvider.generateAccessToken(user);
        final RefreshToken refreshToken = this.tokenProvider.generateRefreshToken(user);

        this.saveRefreshToken(user, session, refreshToken, now);

        final long expiresIn = Duration.between(now, accessToken.getExpiresAt()).getSeconds();

        return LoginResponse.builder()
                .accessToken(accessToken.getToken())
                .refreshToken(refreshToken.getToken())
                .expiresIn(expiresIn)
                .tokenType("Bearer")
                .build();
    }

    private void revokeActiveTokensForSession(final DeviceSession session, final Instant now) {
        if (session == null || session.getId() == null) {
            return;
        }
        this.refreshTokenRepository.revokeActiveBySessionId(session.getId(), now, LOGIN_REVOKE_REASON);
    }

    private DeviceSession resolveSession(final AuthUser user, final LoginRequest loginRequest, final Instant now) {
        final Optional<DeviceSession> existingSession = this.deviceSessionRepository
                .findByUserIdAndSessionSourceId(user.getId(), loginRequest.getSessionSourceId());

        if (existingSession.isPresent()) {
            final DeviceSession session = existingSession.get();
            final boolean updated = this.refreshSession(session, loginRequest, now);
            return updated ? this.deviceSessionRepository.save(session) : session;
        }

        final DeviceSession session = this.createSession(user, loginRequest, now);
        return this.deviceSessionRepository.save(session);
    }

    private boolean refreshSession(final DeviceSession existing,
                                   final LoginRequest loginRequest,
                                   final Instant now) {
        boolean updated = false;
        if (existing.getStatus() != SessionStatus.ACTIVE) {
            existing.setStatus(SessionStatus.ACTIVE);
            updated = true;
        }
        if (this.shouldUpdateLastUsedAt(existing, now)) {
            existing.setLastUsedAt(now);
            updated = true;
        }
        if (!Objects.equals(existing.getLastUserAgent(), loginRequest.getUserAgent())) {
            existing.setLastUserAgent(loginRequest.getUserAgent());
            updated = true;
        }
        return updated;
    }

    private DeviceSession createSession(final AuthUser user,
                                        final LoginRequest loginRequest,
                                        final Instant now) {
        return DeviceSession.builder()
                .id(UUID.randomUUID())
                .user(user)
                .sessionSourceId(loginRequest.getSessionSourceId())
                .status(SessionStatus.ACTIVE)
                .createdAt(now)
                .lastUsedAt(now)
                .initialUserAgent(loginRequest.getUserAgent())
                .lastUserAgent(loginRequest.getUserAgent())
                .build();
    }

    private void saveRefreshToken(final AuthUser user,
                                  final DeviceSession session,
                                  final RefreshToken refreshToken,
                                  final Instant now) {
        this.refreshTokenRepository.save(RefreshTokenRecord.builder()
                .tokenHash(this.tokenHasher.hash(refreshToken.getToken()))
                .user(user)
                .session(session)
                .status(RefreshTokenStatus.ACTIVE)
                .expiresAt(refreshToken.getExpiresAt())
                .createdAt(now)
                .updatedAt(now)
                .build());
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
