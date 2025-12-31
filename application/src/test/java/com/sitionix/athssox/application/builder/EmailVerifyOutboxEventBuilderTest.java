package com.sitionix.athssox.application.builder;

import com.sitionix.athssox.domain.model.outbox.OutboxAggregateType;
import com.sitionix.athssox.domain.model.outbox.OutboxBuildContext;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.InitiatorType;
import com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate;
import com.sitionix.athssox.domain.model.outbox.payload.VerifyChannel;
import com.sitionix.athssox.domain.service.EmailVerificationTokenService;
import com.sitionix.athssox.domain.service.VerificationLinkFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerifyOutboxEventBuilderTest {

    private EmailVerifyOutboxEventBuilder builder;

    @Mock
    private EmailVerificationTokenService tokenService;

    @Mock
    private VerificationLinkFactory linkFactory;

    @BeforeEach
    void setUp() {
        this.builder = new EmailVerifyOutboxEventBuilder(this.tokenService, this.linkFactory);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.tokenService, this.linkFactory);
    }

    @Test
    void given_context_when_build_then_return_email_verify_outbox_event() {
        //given
        final UUID siteId = UUID.randomUUID();
        final Instant requestedAt = Instant.now();
        final OutboxBuildContext given = this.getOutboxBuildContext(siteId,
                requestedAt);

        when(this.tokenService.issue(1L, siteId))
                .thenReturn("rawToken");
        when(this.linkFactory.buildEmailVerifyUrl("rawToken", siteId))
                .thenReturn("verifyUrl");

        //when
        final OutboxEvent<EmailVerifyPayload> actual = this.builder.build(given);

        //then
        final OutboxEvent<EmailVerifyPayload> expected = this.getOutboxEvent(actual.getNextRetryAt(),
                this.getEmailVerifyPayload(siteId, requestedAt));

        assertThat(actual).isEqualTo(expected);

        verify(this.tokenService)
                .issue(1L, siteId);
        verify(this.linkFactory)
                .buildEmailVerifyUrl("rawToken", siteId);
    }

    @Test
    void given_builder_when_event_type_then_return_email_verify() {
        //given

        //when
        final OutboxEventType actual = this.builder.eventType();

        //then
        assertThat(actual).isEqualTo(OutboxEventType.EMAIL_VERIFY);
    }

    private OutboxBuildContext getOutboxBuildContext(final UUID siteId,
                                                     final Instant requestedAt) {
        return new OutboxBuildContext(1L,
                siteId,
                "email",
                "traceId",
                null,
                requestedAt);
    }

    private OutboxEvent<EmailVerifyPayload> getOutboxEvent(final LocalDateTime nextRetryAt,
                                                           final EmailVerifyPayload payload) {
        return OutboxEvent.<EmailVerifyPayload>builder()
                .aggregateType(OutboxAggregateType.USER)
                .aggregateId(1L)
                .initiatorType(InitiatorType.USER)
                .initiatorId("1")
                .eventType(OutboxEventType.EMAIL_VERIFY)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(nextRetryAt)
                .payload(payload)
                .lastError(null)
                .build();
    }

    private EmailVerifyPayload getEmailVerifyPayload(final UUID siteId,
                                                     final Instant requestedAt) {
        return EmailVerifyPayload.builder()
                .delivery(this.getDelivery())
                .template(NotificationTemplate.EMAIL_VERIFY)
                .params(this.getParams())
                .meta(this.getMeta(siteId, requestedAt))
                .build();
    }

    private EmailVerifyPayload.Delivery getDelivery() {
        return EmailVerifyPayload.Delivery.builder()
                .channel(VerifyChannel.EMAIL)
                .to("email")
                .build();
    }

    private EmailVerifyPayload.Params getParams() {
        return EmailVerifyPayload.Params.builder()
                .verifyUrl("verifyUrl")
                .build();
    }

    private EmailVerifyPayload.Meta getMeta(final UUID siteId, final Instant instant) {
        return EmailVerifyPayload.Meta.builder()
                .userId(1L)
                .siteId(siteId)
                .traceId("traceId")
                .requestedAt(instant)
                .build();
    }
}
