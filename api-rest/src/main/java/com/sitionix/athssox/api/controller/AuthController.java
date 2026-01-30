package com.sitionix.athssox.api.controller;

import com.app_afesox.athssox.api_first.api.AuthApi;
import com.app_afesox.athssox.api_first.dto.EmailVerificationDTO;
import com.app_afesox.athssox.api_first.dto.EmailVerificationResponseDTO;
import com.app_afesox.athssox.api_first.dto.IssueEmailVerificationLinkResponseDTO;
import com.app_afesox.athssox.api_first.dto.LoginRequestDTO;
import com.app_afesox.athssox.api_first.dto.LoginResponseDTO;
import com.app_afesox.athssox.api_first.dto.RefreshAccessTokenRequestDTO;
import com.app_afesox.athssox.api_first.dto.RefreshAccessTokenResponseDTO;
import com.sitionix.athssox.api.mapper.AuthApiMapper;
import com.sitionix.athssox.api.mapper.EmailVerificationLinkApiMapper;
import com.sitionix.athssox.api.mapper.EmailVerifyApiMapper;
import com.sitionix.athssox.api.mapper.RefreshAccessTokenApiMapper;
import com.sitionix.athssox.domain.model.LoginRequest;
import com.sitionix.athssox.domain.model.LoginResponse;
import com.sitionix.athssox.domain.model.RefreshAccessTokenRequest;
import com.sitionix.athssox.domain.model.RefreshAccessTokenResponse;
import com.sitionix.athssox.domain.model.emailverify.EmailVerification;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationLinkIssue;
import com.sitionix.athssox.domain.usecase.IssueEmailVerificationLink;
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

import java.util.UUID;

@Log4j2
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthApiMapper authApiMapper;

    private final EmailVerifyApiMapper emailVerifyApiMapper;

    private final EmailVerificationLinkApiMapper emailVerificationLinkApiMapper;

    private final VerifyEmail verifyEmail;

    private final LoginUser loginUser;

    private final RefreshAccessToken refreshAccessToken;

    private final RefreshAccessTokenApiMapper refreshAccessTokenApiMapper;

    private final HttpServletRequest httpServletRequest;

    private final IssueEmailVerificationLink issueEmailVerificationLink;

    @Override
    public ResponseEntity<LoginResponseDTO> login(@Valid final LoginRequestDTO loginRequestDTO) {
        log.info("Received login request for email: {}", loginRequestDTO.getEmail());

        final LoginRequest loginRequest = this.authApiMapper.asLoginRequest(loginRequestDTO);
        loginRequest.setUserAgent(this.httpServletRequest.getHeader(HttpHeaders.USER_AGENT));
        final LoginResponse loginResponse = this.loginUser.execute(loginRequest);

        log.info("Login completed for email: {}", loginRequestDTO.getEmail());
        return ResponseEntity.ok(this.authApiMapper.asLoginResponseDTO(loginResponse));
    }

    @Override
    public ResponseEntity<EmailVerificationResponseDTO> verifyEmail(@Valid final EmailVerificationDTO emailVerificationDTO) {
        log.info("Received email verification request for siteId: {}", emailVerificationDTO.getSiteId());
        final EmailVerification emailVerification = this.emailVerifyApiMapper.asEmailVerification(emailVerificationDTO);

        final boolean verified = this.verifyEmail.execute(emailVerification);

        final EmailVerificationResponseDTO response = this.emailVerifyApiMapper.asEmailVerificationResponseDTO(verified);

        return verified
                ? ResponseEntity.ok(response)
                : ResponseEntity.accepted().body(response);
    }

    @Override
    public ResponseEntity<RefreshAccessTokenResponseDTO> refreshAccessToken(@Valid final RefreshAccessTokenRequestDTO refreshAccessTokenRequest) {
        final RefreshAccessTokenRequest request = this.refreshAccessTokenApiMapper.asRefreshAccessTokenRequest(refreshAccessTokenRequest);

        request.setUserAgent(this.httpServletRequest.getHeader(HttpHeaders.USER_AGENT));

        final RefreshAccessTokenResponse response = this.refreshAccessToken.execute(request);

        return ResponseEntity.ok(this.refreshAccessTokenApiMapper.asRefreshAccessTokenResponseDTO(response));
    }

    @Override
    public ResponseEntity<IssueEmailVerificationLinkResponseDTO> issueEmailVerificationLink(final UUID id, final UUID pepperId) {
        log.info("Issuing email verification link.");

        final EmailVerificationLinkIssue issue = this.issueEmailVerificationLink.execute(id, pepperId);
        final IssueEmailVerificationLinkResponseDTO response = this.emailVerificationLinkApiMapper.asResponse(issue);

        return ResponseEntity.ok(response);
    }
}
