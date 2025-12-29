package com.sitionix.athssox.postgresql.mapper.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OutboxPayloadJsonMapperTest {

    private ObjectMapper objectMapper;
    private OutboxPayloadJsonMapper mapper;

    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper();
        this.mapper = new OutboxPayloadJsonMapper(this.objectMapper);
    }

    @Test
    void givenNullPayload_whenAsJson_thenReturnNull() {
        //given
        final Object given = null;

        //when
        final String actual = this.mapper.asJson(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void givenPayload_whenAsJson_thenReturnJsonString() throws Exception {
        //given
        final SamplePayload given = this.getSamplePayload("verify", 2);
        final String expected = this.objectMapper.writeValueAsString(given);

        //when
        final String actual = this.mapper.asJson(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private SamplePayload getSamplePayload(final String name, final int count) {
        return new SamplePayload(name, count);
    }

    private static class SamplePayload {
        private final String name;
        private final int count;

        private SamplePayload(final String name, final int count) {
            this.name = name;
            this.count = count;
        }

        public String getName() {
            return this.name;
        }

        public int getCount() {
            return this.count;
        }
    }
}
