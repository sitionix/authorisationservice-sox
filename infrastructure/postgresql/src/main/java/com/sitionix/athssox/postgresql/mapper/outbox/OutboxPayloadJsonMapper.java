package com.sitionix.athssox.postgresql.mapper.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class OutboxPayloadJsonMapper {

    private final ObjectMapper objectMapper;

    public OutboxPayloadJsonMapper(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Named("toJson")
    public String asJson(final Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return this.objectMapper.writeValueAsString(payload);
        } catch (final JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize outbox payload", exception);
        }
    }
}
