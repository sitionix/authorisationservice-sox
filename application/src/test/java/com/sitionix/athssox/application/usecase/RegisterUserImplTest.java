package com.sitionix.athssox.application.usecase;

import com.sitionix.athssox.domain.builder.OutboxEventBuilder;
import com.sitionix.athssox.domain.command.OutboxCommand;
import com.sitionix.athssox.domain.model.RegisterUserDO;
import com.sitionix.athssox.domain.model.ResponseRegisterUser;
import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.domain.model.UserStatus;
import com.sitionix.athssox.domain.exception.EmailAlreadyRegisteredException;
import com.sitionix.athssox.domain.exception.MissingSiteIdException;
import com.sitionix.athssox.domain.model.outbox.OutboxBuildContext;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.repository.UserRepository;
import com.sitionix.athssox.domain.service.EmailVerificationResendPolicy;
import com.sitionix.athssox.application.validator.PasswordPolicyValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    private OutboxCommand<EmailVerifyPayload> outboxCommand;

    @Mock
    private OutboxEventBuilder<EmailVerifyPayload> outboxEventBuilder;

    @Mock
    private EmailVerificationResendPolicy emailVerificationResendPolicy;

    @BeforeEach
    void setUp() {
        this.registerUser = new RegisterUserImpl(this.userRepository,
                this.passwordEncoder,
                this.passwordPolicyValidator,
                this.outboxCommand,
                this.outboxEventBuilder,
                this.emailVerificationResendPolicy);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.userRepository,
                this.passwordEncoder,
                this.passwordPolicyValidator,
                this.outboxCommand,
                this.outboxEventBuilder,
                this.emailVerificationResendPolicy);
    }

    @Test
    void given_register_user_do_when_execute_then_encode_password_create_user_and_return_response_with_message() {
        //given
        final String rawPassword = "StrongPassword123";
        final String encodedPassword = "encoded";
        final UUID siteId = this.getSiteId();
        final RegisterUserDO given = this.getRegisterUserDO(siteId,
                DEFAULT_EMAIL,
                UserRole.SITE_USER,
                UserStatus.PENDING_EMAIL_VERIFY,
                rawPassword);

        final ResponseRegisterUser createdUser = this.getResponseRegisterUser(10L,
                UserStatus.PENDING_EMAIL_VERIFY,
                null);
        final ResponseRegisterUser expected = this.getResponseRegisterUser(10L,
                UserStatus.PENDING_EMAIL_VERIFY,
                "Registration accepted. Please check your email for verification.");
        final OutboxEvent<EmailVerifyPayload> outboxEvent = mock(OutboxEvent.class);

        when(this.userRepository.findSiteScopedByEmailAndSiteId(DEFAULT_EMAIL, siteId))
                .thenReturn(Optional.empty());
        when(this.passwordEncoder.encode(rawPassword))
                .thenReturn(encodedPassword);
        when(this.userRepository.createUser(given))
                .thenReturn(createdUser);
        when(this.outboxEventBuilder.build(any(OutboxBuildContext.class)))
                .thenReturn(outboxEvent);

        //when
        final ResponseRegisterUser actual = this.registerUser.execute(given);

        //then
        final ArgumentCaptor<RegisterUserDO> registerUserCaptor = ArgumentCaptor.forClass(RegisterUserDO.class);
        final ArgumentCaptor<OutboxBuildContext> buildContextCaptor = ArgumentCaptor.forClass(OutboxBuildContext.class);

        verify(this.passwordPolicyValidator)
                .validate(rawPassword);
        verify(this.userRepository)
                .findSiteScopedByEmailAndSiteId(DEFAULT_EMAIL, siteId);
        verify(this.passwordEncoder)
                .encode(rawPassword);
        verify(this.userRepository)
                .createUser(registerUserCaptor.capture());
        verify(this.outboxEventBuilder)
                .build(buildContextCaptor.capture());
        verify(this.outboxCommand)
                .execute(outboxEvent);

        final RegisterUserDO expectedRegisterUserDO = this.getRegisterUserDO(siteId,
                DEFAULT_EMAIL,
                UserRole.SITE_USER,
                UserStatus.PENDING_EMAIL_VERIFY,
                encodedPassword);
        assertThat(registerUserCaptor.getValue()).isEqualTo(expectedRegisterUserDO);
        assertThat(actual).isEqualTo(expected);
        assertThat(buildContextCaptor.getValue().requestedAt()).isNotNull();
        assertThat(buildContextCaptor.getValue()).isEqualTo(this.getOutboxBuildContext(createdUser.getUserId(),
                siteId,
                DEFAULT_EMAIL,
                buildContextCaptor.getValue().requestedAt()));
    }

    @Test
    void given_pending_site_scoped_user_when_execute_then_return_existing_user_and_resend() {
        //given
        final UUID siteId = this.getSiteId();
        final RegisterUserDO given = this.getRegisterUserDO(siteId,
                DEFAULT_EMAIL,
                UserRole.SITE_USER,
                UserStatus.PENDING_EMAIL_VERIFY,
                "weak");
        final ResponseRegisterUser existingUser = this.getResponseRegisterUser(14L,
                UserStatus.PENDING_EMAIL_VERIFY,
                null);
        final ResponseRegisterUser expected = this.getResponseRegisterUser(14L,
                UserStatus.PENDING_EMAIL_VERIFY,
                "Registration accepted. Please check your email for verification.");
        final OutboxEvent<EmailVerifyPayload> outboxEvent = mock(OutboxEvent.class);

        when(this.userRepository.findSiteScopedByEmailAndSiteId(DEFAULT_EMAIL, siteId))
                .thenReturn(Optional.of(existingUser));
        when(this.emailVerificationResendPolicy.isResendAllowed(14L))
                .thenReturn(true);
        when(this.outboxEventBuilder.build(any(OutboxBuildContext.class)))
                .thenReturn(outboxEvent);

        //when
        final ResponseRegisterUser actual = this.registerUser.execute(given);

        //then
        final ArgumentCaptor<OutboxBuildContext> buildContextCaptor = ArgumentCaptor.forClass(OutboxBuildContext.class);

        verify(this.userRepository)
                .findSiteScopedByEmailAndSiteId(DEFAULT_EMAIL, siteId);
        verify(this.emailVerificationResendPolicy)
                .isResendAllowed(14L);
        verify(this.outboxEventBuilder)
                .build(buildContextCaptor.capture());
        verify(this.outboxCommand)
                .execute(outboxEvent);

        assertThat(actual).isEqualTo(expected);
        assertThat(buildContextCaptor.getValue().requestedAt()).isNotNull();
        assertThat(buildContextCaptor.getValue()).isEqualTo(this.getOutboxBuildContext(existingUser.getUserId(),
                siteId,
                DEFAULT_EMAIL,
                buildContextCaptor.getValue().requestedAt()));
    }

    @Test
    void given_pending_site_scoped_user_and_resend_not_allowed_when_execute_then_return_existing_user_without_outbox() {
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
    void given_site_scoped_email_already_registered_when_execute_then_throw_and_do_not_encode_or_create_user() {
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
                this.outboxEventBuilder,
                this.outboxCommand);
    }

    @Test
    void given_global_email_already_registered_when_execute_then_throw_and_do_not_encode_or_create_user() {
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
                this.outboxEventBuilder,
                this.outboxCommand);
    }

    @Test
    void given_invalid_password_when_execute_then_throw_and_do_not_encode_or_create_user() {
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
                this.outboxEventBuilder,
                this.outboxCommand);
    }

    @Test
    void given_site_scoped_role_without_site_id_when_execute_then_throw_missing_site_id_exception() {
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
                this.outboxEventBuilder,
                this.outboxCommand);
    }

    @Test
    void given_global_role_with_site_id_when_execute_then_clear_site_id_and_use_global_uniqueness() {
        //given
        final String rawPassword = this.getPassword();
        final String encodedPassword = this.getEncodedPassword();
        final UUID siteId = this.getSiteId();
        final RegisterUserDO given = this.getRegisterUserDO(siteId,
                DEFAULT_EMAIL,
                UserRole.SUPER_ADMIN,
                UserStatus.PENDING_EMAIL_VERIFY,
                rawPassword);

        final ResponseRegisterUser createdUser = this.getResponseRegisterUser(11L,
                UserStatus.PENDING_EMAIL_VERIFY,
                null);
        final ResponseRegisterUser expected = this.getResponseRegisterUser(11L,
                UserStatus.PENDING_EMAIL_VERIFY,
                "Registration accepted. Please check your email for verification.");
        final OutboxEvent<EmailVerifyPayload> outboxEvent = mock(OutboxEvent.class);

        when(this.userRepository.findGlobalByEmail(DEFAULT_EMAIL))
                .thenReturn(Optional.empty());
        when(this.passwordEncoder.encode(rawPassword))
                .thenReturn(encodedPassword);
        when(this.userRepository.createUser(given))
                .thenReturn(createdUser);
        when(this.outboxEventBuilder.build(any(OutboxBuildContext.class)))
                .thenReturn(outboxEvent);

        //when
        final ResponseRegisterUser actual = this.registerUser.execute(given);

        //then
        final ArgumentCaptor<RegisterUserDO> registerUserCaptor = ArgumentCaptor.forClass(RegisterUserDO.class);
        final ArgumentCaptor<OutboxBuildContext> buildContextCaptor = ArgumentCaptor.forClass(OutboxBuildContext.class);

        verify(this.passwordPolicyValidator)
                .validate(rawPassword);
        verify(this.userRepository)
                .findGlobalByEmail(DEFAULT_EMAIL);
        verify(this.passwordEncoder)
                .encode(rawPassword);
        verify(this.userRepository)
                .createUser(registerUserCaptor.capture());
        verify(this.outboxEventBuilder)
                .build(buildContextCaptor.capture());
        verify(this.outboxCommand)
                .execute(outboxEvent);

        final RegisterUserDO expectedRegisterUserDO = this.getRegisterUserDO(null,
                DEFAULT_EMAIL,
                UserRole.SUPER_ADMIN,
                UserStatus.PENDING_EMAIL_VERIFY,
                encodedPassword);
        assertThat(registerUserCaptor.getValue()).isEqualTo(expectedRegisterUserDO);
        assertThat(actual).isEqualTo(expected);
        assertThat(buildContextCaptor.getValue().requestedAt()).isNotNull();
        assertThat(buildContextCaptor.getValue()).isEqualTo(this.getOutboxBuildContext(createdUser.getUserId(),
                null,
                DEFAULT_EMAIL,
                buildContextCaptor.getValue().requestedAt()));
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

    private OutboxBuildContext getOutboxBuildContext(final Long userId,
                                                     final UUID siteId,
                                                     final String email,
                                                     final Instant requestedAt) {
        return new OutboxBuildContext(userId,
                siteId,
                email,
                null,
                null,
                requestedAt);
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

}
