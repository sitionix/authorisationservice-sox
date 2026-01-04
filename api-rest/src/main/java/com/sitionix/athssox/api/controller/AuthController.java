package com.sitionix.athssox.api.controller;

import com.app_afesox.athssox.api_first.api.AuthApi;
import com.app_afesox.athssox.api_first.dto.EmailVerificationDTO;
import com.app_afesox.athssox.api_first.dto.EmailVerificationResponseDTO;
import com.app_afesox.athssox.api_first.dto.LoginRequestDTO;
import com.app_afesox.athssox.api_first.dto.LoginResponseDTO;
import com.sitionix.athssox.api.mapper.AuthApiMapper;
import com.sitionix.athssox.api.mapper.EmailVerifyApiMapper;
import com.sitionix.athssox.domain.model.LoginRequest;
import com.sitionix.athssox.domain.model.LoginResponse;
import com.sitionix.athssox.domain.model.emailverify.EmailVerification;
import com.sitionix.athssox.domain.usecase.LoginUser;
import com.sitionix.athssox.domain.usecase.VerifyEmail;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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

    @Override
    public ResponseEntity<LoginResponseDTO> login(@Valid final LoginRequestDTO loginRequestDTO) {
        log.info("Received login request for email: {}", loginRequestDTO.getEmail());

        final LoginRequest loginRequest = this.authApiMapper.asLoginRequest(loginRequestDTO);
        final LoginResponse loginResponse = this.loginUser.execute(loginRequest);

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
}
