package com.sitionix.athssox.api.controller;

import com.app_afesox.athssox.api_first.dto.EmailVerificationDTO;
import com.app_afesox.athssox.api_first.dto.EmailVerificationResponseDTO;
import com.app_afesox.athssox.api_first.dto.IssueEmailVerificationLinkResponseDTO;
import com.app_afesox.athssox.api_first.dto.LoginRequestDTO;
import com.app_afesox.athssox.api_first.dto.LoginResponseDTO;
import com.app_afesox.athssox.api_first.dto.RefreshAccessTokenRequestDTO;
import com.app_afesox.athssox.api_first.dto.RefreshAccessTokenResponseDTO;
import com.app_afesox.athssox.api_first.dto.ResendEmailVerificationResponseDTO;
import com.sitionix.athssox.api.mapper.AuthApiMapper;
import com.sitionix.athssox.api.mapper.EmailVerificationLinkApiMapper;
import com.sitionix.athssox.api.mapper.EmailVerifyApiMapper;
import com.sitionix.athssox.api.mapper.RefreshAccessTokenApiMapper;
import com.sitionix.athssox.api.mapper.ResendEmailVerificationApiMapper;
import com.sitionix.athssox.domain.model.LoginRequest;
import com.sitionix.athssox.domain.model.LoginResponse;
import com.sitionix.athssox.domain.model.RefreshAccessTokenRequest;
import com.sitionix.athssox.domain.model.RefreshAccessTokenResponse;
import com.sitionix.athssox.domain.model.ResendEmailVerificationResponse;
import com.sitionix.athssox.domain.model.emailverify.EmailVerification;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationLinkIssue;
import com.sitionix.athssox.domain.usecase.IssueEmailVerificationLink;
import com.sitionix.athssox.domain.usecase.LoginUser;
import com.sitionix.athssox.domain.usecase.RefreshAccessToken;
import com.sitionix.athssox.domain.usecase.ResendEmailVerification;
import com.sitionix.athssox.domain.usecase.VerifyEmail;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

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
    private EmailVerificationLinkApiMapper emailVerificationLinkApiMapper;

    @Mock
    private VerifyEmail verifyEmail;

    @Mock
    private LoginUser loginUser;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private RefreshAccessToken refreshAccessToken;

    @Mock
    private RefreshAccessTokenApiMapper refreshAccessTokenApiMapper;

    @Mock
    private IssueEmailVerificationLink issueEmailVerificationLink;

    @Mock
    private ResendEmailVerification resendEmailVerification;

    @Mock
    private ResendEmailVerificationApiMapper resendEmailVerificationApiMapper;

    @BeforeEach
    void setUp() {
        this.authController = new AuthController(this.authApiMapper,
                this.emailVerifyApiMapper,
                this.emailVerificationLinkApiMapper,
                this.verifyEmail,
                this.loginUser,
                this.refreshAccessToken,
                this.refreshAccessTokenApiMapper,
                this.httpServletRequest,
                this.issueEmailVerificationLink,
                this.resendEmailVerification,
                this.resendEmailVerificationApiMapper);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.authApiMapper,
                this.emailVerifyApiMapper,
                this.emailVerificationLinkApiMapper,
                this.verifyEmail,
                this.loginUser,
                this.refreshAccessToken,
                this.refreshAccessTokenApiMapper,
                this.httpServletRequest,
                this.issueEmailVerificationLink,
                this.resendEmailVerification,
                this.resendEmailVerificationApiMapper);
    }

    @Test
    void givenLoginRequestDto_whenLogin_thenReturnLoginResponseDto() {
        //given
        final LoginRequestDTO given = mock(LoginRequestDTO.class);
        final LoginRequest loginRequest = mock(LoginRequest.class);
        final LoginResponse loginResponse = mock(LoginResponse.class);
        final LoginResponseDTO expected = mock(LoginResponseDTO.class);

        when(this.authApiMapper.asLoginRequest(given))
                .thenReturn(loginRequest);
        when(given.getEmail())
                .thenReturn("user@sitionix.com");
        when(this.httpServletRequest.getHeader(HttpHeaders.USER_AGENT))
                .thenReturn("Mozilla/5.0");
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
        verify(this.httpServletRequest)
                .getHeader(HttpHeaders.USER_AGENT);
        verify(this.loginUser)
                .execute(loginRequest);
        verify(this.authApiMapper)
                .asLoginResponseDTO(loginResponse);
    }

    @Test
    void givenEmailVerificationDto_whenVerifyEmailAndVerified_thenReturnOk() {
        //given
        final EmailVerificationDTO given = mock(EmailVerificationDTO.class);
        final EmailVerification emailVerification = mock(EmailVerification.class);
        final EmailVerificationResponseDTO expected = this.getEmailVerificationResponseDTO("Email verified successfully.",
                EmailVerificationResponseDTO.StatusEnum.ACTIVE);

        when(this.emailVerifyApiMapper.asEmailVerification(given))
                .thenReturn(emailVerification);
        when(this.verifyEmail.execute(emailVerification))
                .thenReturn(true);
        when(this.emailVerifyApiMapper.asEmailVerificationResponseDTO(true))
                .thenReturn(expected);

        //when
        final ResponseEntity<EmailVerificationResponseDTO> actual = this.authController.verifyEmail(given);

        //then
        assertThat(actual).isEqualTo(ResponseEntity.ok(expected));
        verify(this.emailVerifyApiMapper)
                .asEmailVerification(given);
        verify(this.verifyEmail)
                .execute(emailVerification);
        verify(this.emailVerifyApiMapper)
                .asEmailVerificationResponseDTO(true);
    }

    @Test
    void givenEmailVerificationDto_whenVerifyEmailAndNotVerified_thenReturnAccepted() {
        //given
        final EmailVerificationDTO given = mock(EmailVerificationDTO.class);
        final EmailVerification emailVerification = mock(EmailVerification.class);
        final EmailVerificationResponseDTO expected = this.getEmailVerificationResponseDTO("Email verification accepted.",
                null);

        when(this.emailVerifyApiMapper.asEmailVerification(given))
                .thenReturn(emailVerification);
        when(this.verifyEmail.execute(emailVerification))
                .thenReturn(false);
        when(this.emailVerifyApiMapper.asEmailVerificationResponseDTO(false))
                .thenReturn(expected);

        //when
        final ResponseEntity<EmailVerificationResponseDTO> actual = this.authController.verifyEmail(given);

        //then
        assertThat(actual).isEqualTo(ResponseEntity.accepted().body(expected));
        verify(this.emailVerifyApiMapper)
                .asEmailVerification(given);
        verify(this.verifyEmail)
                .execute(emailVerification);
        verify(this.emailVerifyApiMapper)
                .asEmailVerificationResponseDTO(false);
    }

    @Test
    void givenRefreshAccessTokenRequestDto_whenRefreshAccessToken_thenReturnRefreshAccessTokenResponseDto() {
        //given
        final RefreshAccessTokenRequestDTO given = mock(RefreshAccessTokenRequestDTO.class);
        final RefreshAccessTokenRequest refreshAccessTokenRequest = mock(RefreshAccessTokenRequest.class);
        final RefreshAccessTokenResponse refreshAccessTokenResponse = mock(RefreshAccessTokenResponse.class);
        final RefreshAccessTokenResponseDTO expected = mock(RefreshAccessTokenResponseDTO.class);

        when(this.refreshAccessTokenApiMapper.asRefreshAccessTokenRequest(given))
                .thenReturn(refreshAccessTokenRequest);
        when(this.httpServletRequest.getHeader(HttpHeaders.USER_AGENT))
                .thenReturn("Mozilla/5.0");
        when(this.refreshAccessToken.execute(refreshAccessTokenRequest))
                .thenReturn(refreshAccessTokenResponse);
        when(this.refreshAccessTokenApiMapper.asRefreshAccessTokenResponseDTO(refreshAccessTokenResponse))
                .thenReturn(expected);

        //when
        final ResponseEntity<RefreshAccessTokenResponseDTO> actual = this.authController.refreshAccessToken(given);

        //then
        assertThat(actual).isEqualTo(ResponseEntity.ok(expected));
        verify(this.refreshAccessTokenApiMapper)
                .asRefreshAccessTokenRequest(given);
        verify(this.httpServletRequest)
                .getHeader(HttpHeaders.USER_AGENT);
        verify(refreshAccessTokenRequest)
                .setUserAgent("Mozilla/5.0");
        verify(this.refreshAccessToken)
                .execute(refreshAccessTokenRequest);
        verify(this.refreshAccessTokenApiMapper)
                .asRefreshAccessTokenResponseDTO(refreshAccessTokenResponse);
        verifyNoMoreInteractions(refreshAccessTokenRequest);
    }

    @Test
    void givenTokenIdAndPepperId_whenIssueEmailVerificationLink_thenReturnResponse() {
        //given
        final UUID tokenId = this.getTokenId();
        final UUID pepperId = this.getPepperId();
        final EmailVerificationLinkIssue issue = this.getEmailVerificationLinkIssue(tokenId);
        final IssueEmailVerificationLinkResponseDTO expected = mock(IssueEmailVerificationLinkResponseDTO.class);

        when(this.issueEmailVerificationLink.execute(tokenId, pepperId))
                .thenReturn(issue);
        when(this.emailVerificationLinkApiMapper.asResponse(issue))
                .thenReturn(expected);

        //when
        final ResponseEntity<IssueEmailVerificationLinkResponseDTO> actual =
                this.authController.issueEmailVerificationLink(tokenId, pepperId);

        //then
        assertThat(actual).isEqualTo(ResponseEntity.ok(expected));
        verify(this.issueEmailVerificationLink)
                .execute(tokenId, pepperId);
        verify(this.emailVerificationLinkApiMapper)
                .asResponse(issue);
    }

    @Test
    void givenAccessToken_whenResendEmailVerification_thenReturnAcceptedResponse() {
        //given
        final Object body = new Object();
        final ResendEmailVerificationResponse response = mock(ResendEmailVerificationResponse.class);
        final ResendEmailVerificationResponseDTO responseDTO = mock(ResendEmailVerificationResponseDTO.class);

        when(this.resendEmailVerification.execute())
                .thenReturn(response);
        when(this.resendEmailVerificationApiMapper.asResendEmailVerificationResponseDTO(response))
                .thenReturn(responseDTO);

        //when
        final ResponseEntity<ResendEmailVerificationResponseDTO> actual =
                this.authController.resendEmailVerification(body);

        //then
        assertThat(actual).isEqualTo(ResponseEntity.accepted().body(responseDTO));
        verify(this.resendEmailVerification)
                .execute();
        verify(this.resendEmailVerificationApiMapper)
                .asResendEmailVerificationResponseDTO(response);
    }

    private EmailVerificationResponseDTO getEmailVerificationResponseDTO(final String message,
                                                                         final EmailVerificationResponseDTO.StatusEnum status) {
        return EmailVerificationResponseDTO.builder()
                .message(message)
                .status(status)
                .build();
    }

    private UUID getTokenId() {
        return UUID.fromString("8f24d9f6-2c05-4b77-8c4e-1bc6e1ba9b6c");
    }

    private UUID getPepperId() {
        return UUID.fromString("d5d2d5de-6930-43c0-9e45-9a8e6dbe8292");
    }

    private EmailVerificationLinkIssue getEmailVerificationLinkIssue(final UUID tokenId) {
        return new EmailVerificationLinkIssue(tokenId,
                UUID.fromString("1a546d09-1161-48bd-9b2a-1d2f416aaf2f"),
                "token",
                Instant.parse("2024-05-01T10:15:30Z"));
    }
}
