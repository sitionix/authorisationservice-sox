package com.sitionix.athssox.application.usecase;

import com.sitionix.athssox.domain.builder.OutboxEventBuilder;
import com.sitionix.athssox.domain.command.OutboxCommand;
import com.sitionix.athssox.domain.exception.EmailVerificationResendNotAllowedException;
import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.model.ResendEmailVerificationResponse;
import com.sitionix.athssox.domain.model.UserStatus;
import com.sitionix.athssox.domain.model.outbox.OutboxBuildContext;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.repository.AuthUserRepository;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.domain.service.EmailVerificationResendPolicy;
import com.sitionix.athssox.domain.usecase.ResendEmailVerification;
import com.sitionix.forge.security.server.user.ForgeUserClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResendEmailVerificationImplTest {

    private ResendEmailVerification resendEmailVerification;

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private EmailVerificationResendPolicy emailVerificationResendPolicy;

    @Mock
    private OutboxCommand<EmailVerifyPayload> outboxCommand;

    @Mock
    private OutboxEventBuilder<EmailVerifyPayload> outboxEventBuilder;

    @Mock
    private Clock clock;

    @Mock
    private ForgeUserClient forgeUserClient;

    @BeforeEach
    void setUp() {
        this.resendEmailVerification = new ResendEmailVerificationImpl(this.authUserRepository,
                this.emailVerificationTokenRepository,
                this.emailVerificationResendPolicy,
                this.outboxCommand,
                this.outboxEventBuilder,
                this.clock,
                this.forgeUserClient);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.authUserRepository,
                this.emailVerificationTokenRepository,
                this.emailVerificationResendPolicy,
                this.outboxCommand,
                this.outboxEventBuilder,
                this.clock,
                this.forgeUserClient);
    }

    @Test
    void givenUserNotFound_whenExecute_thenReturnAcceptedResponse() {
        //given
        final Long userId = 12L;
        final ResendEmailVerificationResponse expected = this.resendEmailVerificationResponse();

        when(this.forgeUserClient.getUserId())
                .thenReturn(userId);
        when(this.authUserRepository.findById(userId))
                .thenReturn(Optional.empty());

        //when
        final ResendEmailVerificationResponse actual = this.resendEmailVerification.execute();

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.forgeUserClient).getUserId();
        verify(this.authUserRepository).findById(userId);
    }

    @Test
    void givenUserNotPending_whenExecute_thenReturnAcceptedResponse() {
        //given
        final Long userId = 7L;
        final ResendEmailVerificationResponse expected = this.resendEmailVerificationResponse();
        final AuthUser user = this.getAuthUser(userId, UserStatus.ACTIVE);

        when(this.forgeUserClient.getUserId())
                .thenReturn(userId);
        when(this.authUserRepository.findById(userId))
                .thenReturn(Optional.of(user));

        //when
        final ResendEmailVerificationResponse actual = this.resendEmailVerification.execute();

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.forgeUserClient).getUserId();
        verify(this.authUserRepository).findById(userId);
    }

    @Test
    void givenPendingUserAndResendNotAllowed_whenExecute_thenThrowException() {
        //given
        final Long userId = 4L;
        final AuthUser user = this.getAuthUser(userId, UserStatus.PENDING_EMAIL_VERIFY);

        when(this.forgeUserClient.getUserId())
                .thenReturn(userId);
        when(this.authUserRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(this.emailVerificationResendPolicy.isResendAllowed(userId))
                .thenReturn(false);

        //when
        final Throwable actual = catchThrowable(() -> this.resendEmailVerification.execute());

        //then
        assertThat(actual).isInstanceOf(EmailVerificationResendNotAllowedException.class);
        verify(this.forgeUserClient).getUserId();
        verify(this.authUserRepository).findById(userId);
        verify(this.emailVerificationResendPolicy).isResendAllowed(userId);
    }

    @Test
    void givenPendingUserAndResendAllowed_whenExecute_thenRevokeAndCreateOutboxEvent() {
        //given
        final Long userId = 3L;
        final AuthUser user = this.getAuthUser(userId, UserStatus.PENDING_EMAIL_VERIFY);
        final Instant now = Instant.parse("2024-01-02T10:15:30Z");
        final OutboxEvent<EmailVerifyPayload> outboxEvent = mock(OutboxEvent.class);
        final ResendEmailVerificationResponse expected = this.resendEmailVerificationResponse();

        when(this.forgeUserClient.getUserId())
                .thenReturn(userId);
        when(this.authUserRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(this.emailVerificationResendPolicy.isResendAllowed(userId))
                .thenReturn(true);
        when(this.clock.instant())
                .thenReturn(now);
        when(this.outboxEventBuilder.build(any(OutboxBuildContext.class)))
                .thenReturn(outboxEvent);

        //when
        final ResendEmailVerificationResponse actual = this.resendEmailVerification.execute();

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.forgeUserClient).getUserId();
        verify(this.authUserRepository).findById(userId);
        verify(this.emailVerificationResendPolicy).isResendAllowed(userId);
        verify(this.emailVerificationTokenRepository).revokeActiveByUserId(userId);

        final ArgumentCaptor<OutboxBuildContext> contextCaptor = ArgumentCaptor.forClass(OutboxBuildContext.class);
        verify(this.outboxEventBuilder).build(contextCaptor.capture());
        verify(this.outboxCommand).execute(outboxEvent);
        verify(this.clock).instant();
        verifyNoMoreInteractions(outboxEvent);

        final OutboxBuildContext expectedContext = this.getOutboxBuildContext(user, now);
        assertThat(contextCaptor.getValue()).isEqualTo(expectedContext);
    }

    private ResendEmailVerificationResponse resendEmailVerificationResponse() {
        return ResendEmailVerificationResponse.builder()
                .message("If your account requires verification, an email will be sent shortly.")
                .build();
    }

    private AuthUser getAuthUser(final Long userId, final UserStatus status) {
        return AuthUser.builder()
                .id(userId)
                .status(status)
                .email("user@sitionix.com")
                .siteId(UUID.fromString("2f6a6c3c-8b0f-4f58-9c18-2a7d9c889a4f"))
                .build();
    }

    private OutboxBuildContext getOutboxBuildContext(final AuthUser user, final Instant now) {
        return new OutboxBuildContext(
                user.getId(),
                user.getSiteId(),
                user.getEmail(),
                null,
                null,
                now
        );
    }

}
