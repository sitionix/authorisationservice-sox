package com.sitionix.athssox.pipe.producer.mapper;

import com.app_afesox.ntfssox.events.notifications.MetaDTO;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationMetaEventMapperTest {

    private NotificationMetaEventMapper notificationMetaEventMapper;

    @BeforeEach
    void setUp() {
        this.notificationMetaEventMapper = new NotificationMetaEventMapperImpl();
    }

    @Test
    void givenMeta_whenAsMeta_thenReturnMetaDto() {
        //given
        final UUID siteId = this.getSiteId();
        final Instant requestedAt = this.getInstant("2024-04-22T08:15:30Z");
        final EmailVerifyPayload.Meta given = this.getMeta(siteId, requestedAt);
        final MetaDTO expected = this.getMetaDto(siteId, requestedAt);

        //when
        final MetaDTO actual = this.notificationMetaEventMapper.asMeta(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenInstant_whenToDateTime_thenReturnDateTimeString() {
        //given
        final Instant given = this.getInstant("2024-04-22T08:16:30Z");
        final String expected = given.toString();

        //when
        final String actual = this.notificationMetaEventMapper.toDateTime(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenUuid_whenToString_thenReturnString() {
        //given
        final UUID given = this.getSiteId();
        final String expected = given.toString();

        //when
        final String actual = this.notificationMetaEventMapper.toString(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private EmailVerifyPayload.Meta getMeta(final UUID siteId,
                                            final Instant requestedAt) {
        return EmailVerifyPayload.Meta.builder()
                .userId(42L)
                .siteId(siteId)
                .traceId("trace-123")
                .requestedAt(requestedAt)
                .build();
    }

    private MetaDTO getMetaDto(final UUID siteId,
                               final Instant requestedAt) {
        return MetaDTO.newBuilder()
                .setUserId(42L)
                .setSiteId(siteId.toString())
                .setTraceId("trace-123")
                .setRequestedAt(requestedAt.toString())
                .build();
    }

    private UUID getSiteId() {
        return UUID.fromString("9cdeca1b-0580-4b75-9b93-e5b7f786b3c0");
    }

    private Instant getInstant(final String value) {
        return Instant.parse(value);
    }
}
