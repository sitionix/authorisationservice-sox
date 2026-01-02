package com.sitionix.athssox.api.controller;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private AuthController authController;

    @Mock
    private AuthApiMapper authApiMapper;

    @Mock
    private EmailVerifyApiMapper emailVerifyApiMapper;

    @Mock
    private VerifyEmail verifyEmail;

    @Mock
    private LoginUser loginUser;

    @BeforeEach
    void setUp() {
        this.authController = new AuthController(this.authApiMapper,
                this.emailVerifyApiMapper,
                this.verifyEmail,
                this.loginUser);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.authApiMapper,
                this.emailVerifyApiMapper,
                this.verifyEmail,
                this.loginUser);
    }

    @Test
    void given_login_request_dto_when_login_then_return_login_response_dto() {
        //given
        final LoginRequestDTO given = mock(LoginRequestDTO.class);
        final LoginRequest loginRequest = mock(LoginRequest.class);
        final LoginResponse loginResponse = mock(LoginResponse.class);
        final LoginResponseDTO expected = mock(LoginResponseDTO.class);

        when(this.authApiMapper.asLoginRequest(given))
                .thenReturn(loginRequest);
        when(this.loginUser.execute(loginRequest))
                .thenReturn(loginResponse);
        when(this.authApiMapper.asLoginResponseDTO(loginResponse))
                .thenReturn(expected);

        //when
        final ResponseEntity<LoginResponseDTO> actual = this.authController.login(given);

        //then
        assertThat(actual).isEqualTo(ResponseEntity.ok(expected));
        verify(this.authApiMapper)
                .asLoginRequest(given);
        verify(this.loginUser)
                .execute(loginRequest);
        verify(this.authApiMapper)
                .asLoginResponseDTO(loginResponse);
    }

    @Test
    void given_email_verification_dto_when_verify_email_and_verified_then_return_ok() {
        //given
        final EmailVerificationDTO given = mock(EmailVerificationDTO.class);
        final EmailVerification emailVerification = mock(EmailVerification.class);
        final EmailVerificationResponseDTO expected = this.getEmailVerificationResponseDTO("Email verified successfully.",
                EmailVerificationResponseDTO.StatusEnum.ACTIVE);

        when(this.emailVerifyApiMapper.asEmailVerification(given))
                .thenReturn(emailVerification);
        when(this.verifyEmail.execute(emailVerification))
                .thenReturn(true);

        //when
        final ResponseEntity<EmailVerificationResponseDTO> actual = this.authController.verifyEmail(given);

        //then
        assertThat(actual).isEqualTo(ResponseEntity.ok(expected));
        verify(this.emailVerifyApiMapper)
                .asEmailVerification(given);
        verify(this.verifyEmail)
                .execute(emailVerification);
    }

    @Test
    void given_email_verification_dto_when_verify_email_and_not_verified_then_return_accepted() {
        //given
        final EmailVerificationDTO given = mock(EmailVerificationDTO.class);
        final EmailVerification emailVerification = mock(EmailVerification.class);
        final EmailVerificationResponseDTO expected = this.getEmailVerificationResponseDTO("Email verification accepted.",
                null);

        when(this.emailVerifyApiMapper.asEmailVerification(given))
                .thenReturn(emailVerification);
        when(this.verifyEmail.execute(emailVerification))
                .thenReturn(false);

        //when
        final ResponseEntity<EmailVerificationResponseDTO> actual = this.authController.verifyEmail(given);

        //then
        assertThat(actual).isEqualTo(ResponseEntity.accepted().body(expected));
        verify(this.emailVerifyApiMapper)
                .asEmailVerification(given);
        verify(this.verifyEmail)
                .execute(emailVerification);
    }

    private EmailVerificationResponseDTO getEmailVerificationResponseDTO(final String message,
                                                                         final EmailVerificationResponseDTO.StatusEnum status) {
        return EmailVerificationResponseDTO.builder()
                .message(message)
                .status(status)
                .build();
    }
}
