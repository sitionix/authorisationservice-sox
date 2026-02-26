package com.sitionix.athssox.postgresql.mapper.outbox;

import com.sitionix.forge.outbox.core.port.OutboxPayloadCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPayloadJsonMapperTest {

    private OutboxPayloadJsonMapper mapper;

    @Mock
    private OutboxPayloadCodec outboxPayloadCodec;

    @BeforeEach
    void setUp() {
        this.mapper = new OutboxPayloadJsonMapper(this.outboxPayloadCodec);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.outboxPayloadCodec);
    }

    @Test
    void given_null_payload_when_as_json_then_return_null() {
        //given
        final Object given = null;

        //when
        final String actual = this.mapper.asJson(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void given_payload_object_when_as_json_then_delegate_to_codec() {
        //given
        final Object given = mock(Object.class);
        final String expected = "serialized-payload";

        when(this.outboxPayloadCodec.serialize(given))
                .thenReturn(expected);

        //when
        final String actual = this.mapper.asJson(given);

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.outboxPayloadCodec).serialize(given);
    }

    @Test
    void given_raw_json_string_when_as_json_then_return_raw_value() {
        //given
        final String given = "{\"foo\":\"bar\"}";

        //when
        final String actual = this.mapper.asJson(given);

        //then
        assertThat(actual).isEqualTo(given);
    }
}
