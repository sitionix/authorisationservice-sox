package com.sitionix.athssox.api.controller;

import com.app_afesox.athssox.api_first.api.AuthApi;
import com.app_afesox.athssox.api_first.dto.EmailVerificationDTO;
import com.app_afesox.athssox.api_first.dto.EmailVerificationResponseDTO;
import com.app_afesox.athssox.api_first.dto.LoginRequestDTO;
import com.app_afesox.athssox.api_first.dto.LoginResponseDTO;
import com.app_afesox.athssox.api_first.dto.RefreshAccessTokenRequestDTO;
import com.app_afesox.athssox.api_first.dto.RefreshAccessTokenResponseDTO;
import com.sitionix.athssox.api.mapper.AuthApiMapper;
import com.sitionix.athssox.api.mapper.EmailVerifyApiMapper;
import com.sitionix.athssox.api.mapper.RefreshAccessTokenApiMapper;
import com.sitionix.athssox.api.ratelimit.ClientIpResolver;
import com.sitionix.athssox.api.ratelimit.RateLimitGuard;
import com.sitionix.athssox.domain.model.LoginRequest;
import com.sitionix.athssox.domain.model.LoginResponse;
import com.sitionix.athssox.domain.model.RefreshAccessTokenRequest;
import com.sitionix.athssox.domain.model.RefreshAccessTokenResponse;
import com.sitionix.athssox.domain.model.emailverify.EmailVerification;
import com.sitionix.athssox.domain.usecase.LoginUser;
import com.sitionix.athssox.domain.usecase.RefreshAccessToken;
import com.sitionix.athssox.domain.usecase.VerifyEmail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthApiMapper authApiMapper;

    private final EmailVerifyApiMapper emailVerifyApiMapper;

    private final VerifyEmail verifyEmail;

    private final LoginUser loginUser;

    private final RefreshAccessToken refreshAccessToken;

    private final RefreshAccessTokenApiMapper refreshAccessTokenApiMapper;

    private final HttpServletRequest httpServletRequest;

    private final ClientIpResolver clientIpResolver;

    private final RateLimitGuard rateLimitGuard;

    @Override
    public ResponseEntity<LoginResponseDTO> login(@Valid final LoginRequestDTO loginRequestDTO) {
        log.info("Received login request for email: {}", loginRequestDTO.getEmail());

        final String clientIp = this.clientIpResolver.resolve(this.httpServletRequest);
        this.rateLimitGuard.checkLogin(clientIp, loginRequestDTO.getEmail(), loginRequestDTO.getSessionSourceId());

        final LoginRequest loginRequest = this.authApiMapper.asLoginRequest(loginRequestDTO);
        loginRequest.setUserAgent(this.httpServletRequest.getHeader(HttpHeaders.USER_AGENT));
        final LoginResponse loginResponse = this.loginUser.execute(loginRequest);

        this.rateLimitGuard.resetLoginEmail(loginRequestDTO.getEmail());

        log.info("Login completed for email: {}", loginRequestDTO.getEmail());
        return ResponseEntity.ok(this.authApiMapper.asLoginResponseDTO(loginResponse));
    }

    @Override
    public ResponseEntity<EmailVerificationResponseDTO> verifyEmail(@Valid final EmailVerificationDTO emailVerificationDTO) {
        log.info("Received email verification request for siteId: {}", emailVerificationDTO.getSiteId());
        final EmailVerification emailVerification = this.emailVerifyApiMapper.asEmailVerification(emailVerificationDTO);

        final boolean verified = this.verifyEmail.execute(emailVerification);

        final EmailVerificationResponseDTO response = EmailVerificationResponseDTO.builder()
                .message(verified ? "Email verified successfully." : "Email verification accepted.")
                .status(verified ? EmailVerificationResponseDTO.StatusEnum.ACTIVE : null)
                .build();

        return verified
                ? ResponseEntity.ok(response)
                : ResponseEntity.accepted().body(response);
    }

    @Override
    public ResponseEntity<RefreshAccessTokenResponseDTO> refreshAccessToken(@Valid final RefreshAccessTokenRequestDTO refreshAccessTokenRequest) {
        final String clientIp = this.clientIpResolver.resolve(this.httpServletRequest);
        this.rateLimitGuard.checkRefresh(clientIp, refreshAccessTokenRequest.getSessionSourceId());

        final RefreshAccessTokenRequest request = this.refreshAccessTokenApiMapper.asRefreshAccessTokenRequest(refreshAccessTokenRequest);

        request.setUserAgent(this.httpServletRequest.getHeader(HttpHeaders.USER_AGENT));

        final RefreshAccessTokenResponse response = this.refreshAccessToken.execute(request);

        return ResponseEntity.ok(this.refreshAccessTokenApiMapper.asRefreshAccessTokenResponseDTO(response));
    }
}
