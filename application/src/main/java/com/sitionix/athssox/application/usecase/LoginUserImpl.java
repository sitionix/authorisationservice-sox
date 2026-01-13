package com.sitionix.athssox.application.usecase;

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
import com.sitionix.athssox.application.security.LoginAuthenticationToken;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginUserImpl implements LoginUser {

    private final DeviceSessionRepository deviceSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;
    private final TokenHasher tokenHasher;
    private final AuthenticationManager authenticationManager;
    private final Clock clock;

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

    private DeviceSession resolveSession(final AuthUser user, final LoginRequest loginRequest, final Instant now) {
        final Optional<DeviceSession> existingSession = this.deviceSessionRepository
                .findByUserIdAndSessionSourceId(user.getId(), loginRequest.getSessionSourceId());

        final DeviceSession session = existingSession
                .map(existing -> this.refreshSession(existing, loginRequest, now))
                .orElseGet(() -> this.createSession(user, loginRequest, now));

        return this.deviceSessionRepository.save(session);
    }

    private DeviceSession refreshSession(final DeviceSession existing,
                                         final LoginRequest loginRequest,
                                         final Instant now) {
        final SessionStatus updatedStatus = existing.getStatus() == SessionStatus.ACTIVE
                ? existing.getStatus()
                : SessionStatus.ACTIVE;

        existing.setStatus(updatedStatus);
        existing.setLastUsedAt(now);
        existing.setLastUserAgent(loginRequest.getUserAgent());

        return existing;
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
}
