package com.sitionix.athssox.application.usecase;

import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenIssue;
import com.sitionix.athssox.domain.model.RegisterUserDO;
import com.sitionix.athssox.domain.model.ResponseRegisterUser;
import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.domain.model.UserStatus;
import com.sitionix.athssox.domain.exception.EmailAlreadyRegisteredException;
import com.sitionix.athssox.domain.exception.MissingSiteIdException;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.repository.UserRepository;
import com.sitionix.athssox.domain.service.EmailVerificationResendPolicy;
import com.sitionix.athssox.domain.service.EmailVerificationTokenService;
import com.sitionix.athssox.application.validator.PasswordPolicyValidator;
import com.sitionix.forge.outbox.core.port.ForgeOutbox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserImplTest {

    private static final String DEFAULT_EMAIL = "email@sitionix.com";

    private RegisterUserImpl registerUser;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordPolicyValidator passwordPolicyValidator;

    @Mock
    private ForgeOutbox forgeOutbox;

    @Mock
    private EmailVerificationTokenService emailVerificationTokenService;

    @Mock
    private EmailVerificationResendPolicy emailVerificationResendPolicy;

    @Mock
    private Clock clock;

    @BeforeEach
    void setUp() {
        this.registerUser = new RegisterUserImpl(this.userRepository,
                this.passwordEncoder,
                this.passwordPolicyValidator,
                this.forgeOutbox,
                this.emailVerificationTokenService,
                this.emailVerificationResendPolicy,
                this.clock);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.userRepository,
                this.passwordEncoder,
                this.passwordPolicyValidator,
                this.forgeOutbox,
                this.emailVerificationTokenService,
                this.emailVerificationResendPolicy,
                this.clock);
    }

    @Test
    void givenRegisterUserDo_whenExecute_thenEncodePasswordCreateUserAndReturnResponseWithMessage() {
        //given
        final String rawPassword = "StrongPassword123";
        final String encodedPassword = "encoded";
        final UUID siteId = this.getSiteId();
        final RegisterUserDO given = this.getRegisterUserDO(siteId,
                DEFAULT_EMAIL,
                UserRole.SITE_USER,
                UserStatus.PENDING_EMAIL_VERIFY,
                rawPassword);
        final Instant now = this.getNow();

        final ResponseRegisterUser createdUser = this.getResponseRegisterUser(10L,
                UserStatus.PENDING_EMAIL_VERIFY,
                null);
        final ResponseRegisterUser expected = this.getResponseRegisterUser(10L,
                UserStatus.PENDING_EMAIL_VERIFY,
                "Registration accepted. Please check your email for verification.");
        final UUID tokenId = UUID.fromString("c9b1f3f4-12c7-11ec-82a8-0242ac130003");
        final UUID pepperId = UUID.fromString("2cf629c1-1b58-4aa3-a9fd-5e9be2b1d31d");

        when(this.userRepository.findSiteScopedByEmailAndSiteId(DEFAULT_EMAIL, siteId))
                .thenReturn(Optional.empty());
        when(this.passwordEncoder.encode(rawPassword))
                .thenReturn(encodedPassword);
        when(this.userRepository.createUser(given))
                .thenReturn(createdUser);
        when(this.clock.instant())
                .thenReturn(now);
        when(this.emailVerificationTokenService.issue(createdUser.getUserId(), siteId))
                .thenReturn(new EmailVerificationTokenIssue(tokenId, pepperId));

        //when
        final ResponseRegisterUser actual = this.registerUser.execute(given);

        //then
        final ArgumentCaptor<RegisterUserDO> registerUserCaptor = ArgumentCaptor.forClass(RegisterUserDO.class);

        verify(this.passwordPolicyValidator)
                .validate(rawPassword);
        verify(this.userRepository)
                .findSiteScopedByEmailAndSiteId(DEFAULT_EMAIL, siteId);
        verify(this.passwordEncoder)
                .encode(rawPassword);
        verify(this.userRepository)
                .createUser(registerUserCaptor.capture());
        verify(this.clock)
                .instant();
        verify(this.emailVerificationTokenService)
                .issue(createdUser.getUserId(), siteId);
        final ArgumentCaptor<EmailVerifyPayload> payloadCaptor = ArgumentCaptor.forClass(EmailVerifyPayload.class);
        verify(this.forgeOutbox)
                .send(payloadCaptor.capture());

        final RegisterUserDO expectedRegisterUserDO = this.getRegisterUserDO(siteId,
                DEFAULT_EMAIL,
                UserRole.SITE_USER,
                UserStatus.PENDING_EMAIL_VERIFY,
                encodedPassword);
        final EmailVerifyPayload expectedPayload = this.getEmailVerifyPayload(createdUser.getUserId(),
                siteId,
                DEFAULT_EMAIL,
                now,
                tokenId,
                pepperId);
        assertThat(registerUserCaptor.getValue()).isEqualTo(expectedRegisterUserDO);
        assertThat(payloadCaptor.getValue()).isEqualTo(expectedPayload);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenPendingSiteScopedUser_whenExecute_thenReturnExistingUserAndResend() {
        //given
        final UUID siteId = this.getSiteId();
        final RegisterUserDO given = this.getRegisterUserDO(siteId,
                DEFAULT_EMAIL,
                UserRole.SITE_USER,
                UserStatus.PENDING_EMAIL_VERIFY,
                "weak");
        final Instant now = this.getNow();
        final ResponseRegisterUser existingUser = this.getResponseRegisterUser(14L,
                UserStatus.PENDING_EMAIL_VERIFY,
                null);
        final ResponseRegisterUser expected = this.getResponseRegisterUser(14L,
                UserStatus.PENDING_EMAIL_VERIFY,
                "Registration accepted. Please check your email for verification.");
        final UUID tokenId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        final UUID pepperId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        when(this.userRepository.findSiteScopedByEmailAndSiteId(DEFAULT_EMAIL, siteId))
                .thenReturn(Optional.of(existingUser));
        when(this.emailVerificationResendPolicy.isResendAllowed(14L))
                .thenReturn(true);
        when(this.clock.instant())
                .thenReturn(now);
        when(this.emailVerificationTokenService.issue(existingUser.getUserId(), siteId))
                .thenReturn(new EmailVerificationTokenIssue(tokenId, pepperId));

        //when
        final ResponseRegisterUser actual = this.registerUser.execute(given);

        //then
        verify(this.userRepository)
                .findSiteScopedByEmailAndSiteId(DEFAULT_EMAIL, siteId);
        verify(this.emailVerificationResendPolicy)
                .isResendAllowed(14L);
        verify(this.clock)
                .instant();
        verify(this.emailVerificationTokenService)
                .issue(existingUser.getUserId(), siteId);
        final ArgumentCaptor<EmailVerifyPayload> payloadCaptor = ArgumentCaptor.forClass(EmailVerifyPayload.class);
        verify(this.forgeOutbox)
                .send(payloadCaptor.capture());

        final EmailVerifyPayload expectedPayload = this.getEmailVerifyPayload(existingUser.getUserId(),
                siteId,
                DEFAULT_EMAIL,
                now,
                tokenId,
                pepperId);
        assertThat(payloadCaptor.getValue()).isEqualTo(expectedPayload);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenPendingSiteScopedUserAndResendNotAllowed_whenExecute_thenReturnExistingUserWithoutOutbox() {
        //given
        final UUID siteId = UUID.randomUUID();
        final RegisterUserDO given = this.getRegisterUserDO(siteId,
                DEFAULT_EMAIL,
                UserRole.SITE_USER,
                UserStatus.PENDING_EMAIL_VERIFY,
                "weak");
        final ResponseRegisterUser existingUser = this.getResponseRegisterUser(15L,
                UserStatus.PENDING_EMAIL_VERIFY,
                null);
        final ResponseRegisterUser expected = this.getResponseRegisterUser(15L,
                UserStatus.PENDING_EMAIL_VERIFY,
                "Registration accepted. Please check your email for verification.");

        when(this.userRepository.findSiteScopedByEmailAndSiteId(DEFAULT_EMAIL, siteId))
                .thenReturn(Optional.of(existingUser));
        when(this.emailVerificationResendPolicy.isResendAllowed(15L))
                .thenReturn(false);

        //when
        final ResponseRegisterUser actual = this.registerUser.execute(given);

        //then
        verify(this.userRepository)
                .findSiteScopedByEmailAndSiteId(DEFAULT_EMAIL, siteId);
        verify(this.emailVerificationResendPolicy)
                .isResendAllowed(15L);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenSiteScopedEmailAlreadyRegistered_whenExecute_thenThrowAndDoNotEncodeOrCreateUser() {
        //given
        final UUID siteId = UUID.randomUUID();
        final RegisterUserDO given = this.getRegisterUserDO(siteId,
                DEFAULT_EMAIL,
                UserRole.SITE_ADMIN,
                UserStatus.PENDING_EMAIL_VERIFY,
                "StrongPassword123");
        final ResponseRegisterUser existingUser = this.getResponseRegisterUser(21L,
                UserStatus.ACTIVE,
                null);

        when(this.userRepository.findSiteScopedByEmailAndSiteId(DEFAULT_EMAIL, siteId))
                .thenReturn(Optional.of(existingUser));

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.registerUser.execute(given));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Registration already processed. Please check your email.");

        verify(this.passwordPolicyValidator)
                .validate("StrongPassword123");
        verify(this.userRepository)
                .findSiteScopedByEmailAndSiteId(DEFAULT_EMAIL, siteId);
        verifyNoInteractions(this.passwordEncoder,
                this.emailVerificationTokenService,
                this.forgeOutbox);
    }

    @Test
    void givenGlobalEmailAlreadyRegistered_whenExecute_thenThrowAndDoNotEncodeOrCreateUser() {
        //given
        final RegisterUserDO given = this.getRegisterUserDO(null,
                DEFAULT_EMAIL,
                UserRole.SUPER_ADMIN,
                UserStatus.PENDING_EMAIL_VERIFY,
                "StrongPassword123");
        final ResponseRegisterUser existingUser = this.getResponseRegisterUser(33L,
                UserStatus.ACTIVE,
                null);

        when(this.userRepository.findGlobalByEmail(DEFAULT_EMAIL))
                .thenReturn(Optional.of(existingUser));

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.registerUser.execute(given));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Registration already processed. Please check your email.");

        verify(this.passwordPolicyValidator)
                .validate("StrongPassword123");
        verify(this.userRepository)
                .findGlobalByEmail(DEFAULT_EMAIL);
        verifyNoInteractions(this.passwordEncoder,
                this.emailVerificationTokenService,
                this.forgeOutbox);
    }

    @Test
    void givenInvalidPassword_whenExecute_thenThrowAndDoNotEncodeOrCreateUser() {
        //given
        final UUID siteId = UUID.randomUUID();
        final RegisterUserDO given = this.getRegisterUserDO(siteId,
                DEFAULT_EMAIL,
                UserRole.SITE_USER,
                UserStatus.PENDING_EMAIL_VERIFY,
                "weak");

        final RuntimeException expected = this.getRuntimeException("invalid password");

        when(this.userRepository.findSiteScopedByEmailAndSiteId(DEFAULT_EMAIL, siteId))
                .thenReturn(Optional.empty());
        doThrow(expected)
                .when(this.passwordPolicyValidator)
                .validate("weak");

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.registerUser.execute(given));

        //then
        assertThat(actualThrowable).isEqualTo(expected);
        verify(this.passwordPolicyValidator)
                .validate("weak");
        verify(this.userRepository)
                .findSiteScopedByEmailAndSiteId(DEFAULT_EMAIL, siteId);
        verifyNoInteractions(this.passwordEncoder,
                this.emailVerificationTokenService,
                this.forgeOutbox);
    }

    @Test
    void givenSiteScopedRoleWithoutSiteId_whenExecute_thenThrowMissingSiteIdException() {
        //given
        final RegisterUserDO given = this.getRegisterUserDO(null,
                DEFAULT_EMAIL,
                UserRole.SITE_USER,
                UserStatus.PENDING_EMAIL_VERIFY,
                this.getPassword());

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.registerUser.execute(given));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(MissingSiteIdException.class)
                .hasMessage("siteId is required for site-scoped roles");
        verifyNoInteractions(this.passwordPolicyValidator,
                this.passwordEncoder,
                this.userRepository,
                this.emailVerificationTokenService,
                this.forgeOutbox);
    }

    @Test
    void givenGlobalRoleWithSiteId_whenExecute_thenClearSiteIdAndUseGlobalUniqueness() {
        //given
        final String rawPassword = this.getPassword();
        final String encodedPassword = this.getEncodedPassword();
        final UUID siteId = this.getSiteId();
        final RegisterUserDO given = this.getRegisterUserDO(siteId,
                DEFAULT_EMAIL,
                UserRole.SUPER_ADMIN,
                UserStatus.PENDING_EMAIL_VERIFY,
                rawPassword);
        final Instant now = this.getNow();

        final ResponseRegisterUser createdUser = this.getResponseRegisterUser(11L,
                UserStatus.PENDING_EMAIL_VERIFY,
                null);
        final ResponseRegisterUser expected = this.getResponseRegisterUser(11L,
                UserStatus.PENDING_EMAIL_VERIFY,
                "Registration accepted. Please check your email for verification.");
        final UUID tokenId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        final UUID pepperId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        when(this.userRepository.findGlobalByEmail(DEFAULT_EMAIL))
                .thenReturn(Optional.empty());
        when(this.passwordEncoder.encode(rawPassword))
                .thenReturn(encodedPassword);
        when(this.userRepository.createUser(given))
                .thenReturn(createdUser);
        when(this.clock.instant())
                .thenReturn(now);
        when(this.emailVerificationTokenService.issue(createdUser.getUserId(), null))
                .thenReturn(new EmailVerificationTokenIssue(tokenId, pepperId));

        //when
        final ResponseRegisterUser actual = this.registerUser.execute(given);

        //then
        final ArgumentCaptor<RegisterUserDO> registerUserCaptor = ArgumentCaptor.forClass(RegisterUserDO.class);

        verify(this.passwordPolicyValidator)
                .validate(rawPassword);
        verify(this.userRepository)
                .findGlobalByEmail(DEFAULT_EMAIL);
        verify(this.passwordEncoder)
                .encode(rawPassword);
        verify(this.userRepository)
                .createUser(registerUserCaptor.capture());
        verify(this.clock)
                .instant();
        verify(this.emailVerificationTokenService)
                .issue(createdUser.getUserId(), null);
        final ArgumentCaptor<EmailVerifyPayload> payloadCaptor = ArgumentCaptor.forClass(EmailVerifyPayload.class);
        verify(this.forgeOutbox)
                .send(payloadCaptor.capture());

        final RegisterUserDO expectedRegisterUserDO = this.getRegisterUserDO(null,
                DEFAULT_EMAIL,
                UserRole.SUPER_ADMIN,
                UserStatus.PENDING_EMAIL_VERIFY,
                encodedPassword);
        final EmailVerifyPayload expectedPayload = this.getEmailVerifyPayload(createdUser.getUserId(),
                null,
                DEFAULT_EMAIL,
                now,
                tokenId,
                pepperId);
        assertThat(registerUserCaptor.getValue()).isEqualTo(expectedRegisterUserDO);
        assertThat(payloadCaptor.getValue()).isEqualTo(expectedPayload);
        assertThat(actual).isEqualTo(expected);
    }

    private EmailVerifyPayload getEmailVerifyPayload(final Long userId,
                                                     final UUID siteId,
                                                     final String email,
                                                     final Instant requestedAt,
                                                     final UUID tokenId,
                                                     final UUID pepperId) {
        return EmailVerifyPayload.builder()
                .delivery(EmailVerifyPayload.Delivery.builder()
                        .channel(com.sitionix.athssox.domain.model.outbox.payload.VerifyChannel.EMAIL)
                        .to(email)
                        .build())
                .template(com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate.EMAIL_VERIFY)
                .params(EmailVerifyPayload.Params.builder()
                        .emailVerificationTokenId(tokenId)
                        .pepperId(pepperId)
                        .build())
                .meta(EmailVerifyPayload.Meta.builder()
                        .userId(userId)
                        .siteId(siteId)
                        .traceId(null)
                        .requestedAt(requestedAt)
                        .build())
                .build();
    }

    private RegisterUserDO getRegisterUserDO(final UUID siteId,
                                             final String email,
                                             final UserRole role,
                                             final UserStatus status,
                                             final String password) {
        return RegisterUserDO.builder()
                .email(email)
                .role(role)
                .status(status)
                .siteId(siteId)
                .password(password)
                .build();
    }

    private ResponseRegisterUser getResponseRegisterUser(final Long userId,
                                                         final UserStatus status,
                                                         final String message) {
        return ResponseRegisterUser.builder()
                .userId(userId)
                .status(status)
                .message(message)
                .build();
    }

    private RuntimeException getRuntimeException(final String message) {
        return new RuntimeException(message);
    }

    private UUID getSiteId() {
        return UUID.randomUUID();
    }

    private String getPassword() {
        return "StrongPassword123";
    }

    private String getEncodedPassword() {
        return "encoded";
    }

    private Instant getNow() {
        return Instant.parse("2024-05-01T10:15:30Z");
    }

}
