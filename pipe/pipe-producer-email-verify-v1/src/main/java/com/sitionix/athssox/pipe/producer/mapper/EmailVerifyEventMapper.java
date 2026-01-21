package com.sitionix.athssox.pipe.producer.mapper;

import com.app_afesox.athssox.events.emailverify.EmailVerifyEvent;
import com.app_afesox.athssox.events.emailverify.EmailVerifyEventEnvelope;
import com.app_afesox.events.Metadata;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface EmailVerifyEventMapper {

    EmailVerifyEvent asEvent(EmailVerifyPayload payload);

    @Mapping(target = "metadata", source = "event")
    EmailVerifyEventEnvelope asEnvelope(Event<EmailVerifyPayload> event);

    @Mapping(target = "createdAt", source = "createdAt")
    Metadata asMetadata(Event<EmailVerifyPayload> event);

    default String toDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.toString();
    }

    default Long toEpochMillis(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.toEpochMilli();
    }

    default String toString(final UUID value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}
