package com.sitionix.athssox.application.usecase;

import com.sitionix.athssox.domain.model.AccessToken;
import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.model.LoginRequest;
import com.sitionix.athssox.domain.model.LoginResponse;
import com.sitionix.athssox.domain.model.RefreshToken;
import com.sitionix.athssox.domain.model.RefreshTokenRecord;
import com.sitionix.athssox.domain.repository.RefreshTokenRepository;
import com.sitionix.athssox.domain.service.TokenHasher;
import com.sitionix.athssox.domain.service.TokenProvider;
import com.sitionix.athssox.domain.usecase.LoginUser;
import com.sitionix.athssox.application.security.LoginAuthenticationToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginUserImpl implements LoginUser {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;
    private final TokenHasher tokenHasher;
    private final AuthenticationManager authenticationManager;
    private final Clock clock;

    @Override
    public LoginResponse execute(@Valid final LoginRequest loginRequest) {
        final Authentication authentication = this.authenticationManager.authenticate(
                LoginAuthenticationToken.unauthenticated(loginRequest.getEmail(),
                        loginRequest.getPassword(),
                        loginRequest.getSiteId()));
        final AuthUser user = ((LoginAuthenticationToken) authentication).getUser();
        final Instant now = this.clock.instant();
        final AccessToken accessToken = this.tokenProvider.generateAccessToken(user);
        final RefreshToken refreshToken = this.tokenProvider.generateRefreshToken(user);

        this.refreshTokenRepository.save(RefreshTokenRecord.builder()
                .tokenHash(this.tokenHasher.hash(refreshToken.getToken()))
                .userId(user.getId())
                .expiresAt(refreshToken.getExpiresAt())
                .build());

        final long expiresIn = Duration.between(now, accessToken.getExpiresAt()).getSeconds();

        return LoginResponse.builder()
                .accessToken(accessToken.getToken())
                .refreshToken(refreshToken.getToken())
                .expiresIn(expiresIn)
                .tokenType("Bearer")
                .build();
    }
}
