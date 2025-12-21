package com.sitionix.athssox.application.outbox;

import com.sitionix.athssox.application.event.UserRegisteredEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxAggregateType;
import com.sitionix.athssox.domain.model.outbox.OutboxEventCreate;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class UserRegistrationOutboxListenerTest {

    private UserRegistrationOutboxListener listener;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private Clock clock;

    @BeforeEach
    void setUp() {
        this.listener = new UserRegistrationOutboxListener(this.outboxEventRepository,
                this.clock);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.outboxEventRepository,
                this.clock);
    }

    @Test
    void given_user_registered_event_when_on_user_registered_then_create_pending_outbox_event() {
        //given
        final Clock fixedClock = this.getFixedClock();
        final UUID siteId = this.getSiteId();
        final UserRegisteredEvent given = this.getUserRegisteredEvent(10L,
                "email@sitionix.com",
                siteId);

        this.stubClock(fixedClock);

        //when
        this.listener.onUserRegistered(given);

        //then
        final ArgumentCaptor<OutboxEventCreate> captor = ArgumentCaptor.forClass(OutboxEventCreate.class);

        verify(this.outboxEventRepository)
                .create(captor.capture());
        verify(this.clock)
                .instant();
        verify(this.clock)
                .getZone();

        final OutboxEventCreate expected = this.getExpectedOutboxEventCreate(fixedClock,
                given);
        assertThat(captor.getValue()).isEqualTo(expected);
    }

    private void stubClock(final Clock fixedClock) {
        final Instant instant = fixedClock.instant();
        org.mockito.Mockito.when(this.clock.instant()).thenReturn(instant);
        org.mockito.Mockito.when(this.clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    private Clock getFixedClock() {
        return Clock.fixed(Instant.parse("2024-01-01T10:15:30.00Z"), ZoneOffset.UTC);
    }

    private UUID getSiteId() {
        return UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    }

    private UserRegisteredEvent getUserRegisteredEvent(final Long userId,
                                                       final String email,
                                                       final UUID siteId) {
        return new UserRegisteredEvent(userId,
                email,
                siteId);
    }

    private OutboxEventCreate getExpectedOutboxEventCreate(final Clock fixedClock,
                                                           final UserRegisteredEvent event) {
        return OutboxEventCreate.builder()
                .aggregateType(OutboxAggregateType.USER)
                .aggregateId(this.toAggregateId(event.getUserId()))
                .eventType(OutboxEventType.EMAIL_VERIFY)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(LocalDateTime.now(fixedClock))
                .payload(this.getExpectedPayload(event))
                .build();
    }

    private UUID toAggregateId(final Long userId) {
        return UUID.nameUUIDFromBytes(("user:" + userId).getBytes(StandardCharsets.UTF_8));
    }

    private String getExpectedPayload(final UserRegisteredEvent event) {
        return "{"
                + "\"delivery\":{\"channel\":\"EMAIL\",\"to\":\"" + event.getEmail() + "\"},"
                + "\"template\":\"EMAIL_VERIFY\","
                + "\"params\":{},"
                + "\"meta\":{\"userId\":\"" + event.getUserId() + "\",\"siteId\":\"" + event.getSiteId() + "\"}"
                + "}";
    }
}
