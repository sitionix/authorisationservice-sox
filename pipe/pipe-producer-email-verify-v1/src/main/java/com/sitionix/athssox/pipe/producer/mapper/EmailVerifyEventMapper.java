package com.sitionix.athssox.pipe.producer.mapper;

import com.app_afesox.events.Metadata;
import com.app_afesox.ntfssox.events.notifications.NotificationEvent;
import com.app_afesox.ntfssox.events.notifications.NotificationEnvelope;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.Event;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {
                EmailVerifyContentEventMapper.class,
                EmailVerifyDeliveryEventMapper.class,
                EmailVerifyMetaEventMapper.class,
                EmailVerifyTemplateEventMapper.class
        })
public interface EmailVerifyEventMapper {

    @Mapping(target = "content", source = "params")
    NotificationEvent asEvent(EmailVerifyPayload payload);

    @Mapping(target = "metadata", source = "event")
    NotificationEnvelope asEnvelope(Event<EmailVerifyPayload> event);

    @Mapping(target = "idempotencyId", source = "idempotencyId", qualifiedByName = "uuidToString")
    @Mapping(target = "createdAt", source = "createdAt")
    Metadata asMetadata(Event<EmailVerifyPayload> event);

    default String toDateTime(final Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.toString();
    }

    default Long toEpochMillis(final Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.toEpochMilli();
    }

    @Named("uuidToString")
    default String toString(final UUID value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}
