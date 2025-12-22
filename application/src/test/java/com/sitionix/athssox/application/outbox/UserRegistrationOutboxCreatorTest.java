package com.sitionix.athssox.application.outbox;

import com.sitionix.athssox.domain.model.RegisterUserDO;
import com.sitionix.athssox.domain.model.ResponseRegisterUser;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationOutboxCreatorTest {

    private UserRegistrationOutboxCreator outboxCreator;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private Clock clock;

    @BeforeEach
    void setUp() {
        this.outboxCreator = new UserRegistrationOutboxCreator(this.outboxEventRepository,
                this.clock);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.outboxEventRepository,
                this.clock);
    }

    @Test
    void given_register_user_do_and_created_user_when_create_then_persist_outbox_event() {
        //given
        final Clock fixedClock = this.getFixedClock();
        final RegisterUserDO registerUserDO = this.getRegisterUserDO();
        final ResponseRegisterUser createdUser = this.getResponseRegisterUser();

        this.stubClock(fixedClock);

        //when
        this.outboxCreator.create(registerUserDO, createdUser);

        //then
        final ArgumentCaptor<OutboxEventCreate> captor = ArgumentCaptor.forClass(OutboxEventCreate.class);

        verify(this.outboxEventRepository)
                .create(captor.capture());
        verify(this.clock)
                .instant();
        verify(this.clock)
                .getZone();

        final OutboxEventCreate expected = this.getExpectedOutboxEventCreate(fixedClock,
                registerUserDO,
                createdUser);
        assertThat(captor.getValue()).isEqualTo(expected);
    }

    private void stubClock(final Clock fixedClock) {
        final Instant instant = fixedClock.instant();
        when(this.clock.instant()).thenReturn(instant);
        when(this.clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    private Clock getFixedClock() {
        return Clock.fixed(Instant.parse("2024-01-01T10:15:30.00Z"), ZoneOffset.UTC);
    }

    private RegisterUserDO getRegisterUserDO() {
        return RegisterUserDO.builder()
                .email("email@sitionix.com")
                .siteId(this.getSiteId())
                .build();
    }

    private ResponseRegisterUser getResponseRegisterUser() {
        return ResponseRegisterUser.builder()
                .userId(10L)
                .build();
    }

    private UUID getSiteId() {
        return UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    }

    private OutboxEventCreate getExpectedOutboxEventCreate(final Clock fixedClock,
                                                           final RegisterUserDO registerUserDO,
                                                           final ResponseRegisterUser createdUser) {
        return OutboxEventCreate.builder()
                .aggregateType(OutboxAggregateType.USER)
                .aggregateId(this.toAggregateId(createdUser.getUserId()))
                .eventType(OutboxEventType.EMAIL_VERIFY)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(LocalDateTime.now(fixedClock))
                .payload(this.getExpectedPayload(registerUserDO, createdUser))
                .build();
    }

    private UUID toAggregateId(final Long userId) {
        return UUID.nameUUIDFromBytes(("user:" + userId).getBytes(StandardCharsets.UTF_8));
    }

    private String getExpectedPayload(final RegisterUserDO registerUserDO,
                                      final ResponseRegisterUser createdUser) {
        return "{"
                + "\"delivery\":{\"channel\":\"EMAIL\",\"to\":\"" + registerUserDO.getEmail() + "\"},"
                + "\"template\":\"EMAIL_VERIFY\","
                + "\"params\":{},"
                + "\"meta\":{\"userId\":\"" + createdUser.getUserId() + "\",\"siteId\":\"" + registerUserDO.getSiteId() + "\"}"
                + "}";
    }
}
