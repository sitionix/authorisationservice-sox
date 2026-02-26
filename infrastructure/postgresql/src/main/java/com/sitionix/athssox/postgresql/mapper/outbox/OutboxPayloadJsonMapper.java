package com.sitionix.athssox.postgresql.mapper.outbox;

import com.sitionix.forge.outbox.core.port.OutboxPayloadCodec;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class OutboxPayloadJsonMapper {

    private final OutboxPayloadCodec outboxPayloadCodec;

    public OutboxPayloadJsonMapper(final OutboxPayloadCodec outboxPayloadCodec) {
        this.outboxPayloadCodec = outboxPayloadCodec;
    }

    @Named("toJson")
    public String asJson(final Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof String rawPayload) {
            return rawPayload;
        }
        return this.outboxPayloadCodec.serialize(payload);
    }
}
