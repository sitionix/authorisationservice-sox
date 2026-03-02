package com.sitionix.athssox.pipe.producer.mapper;

import com.app_afesox.events.Metadata;
import com.app_afesox.ntfssox.events.notifications.NotificationEvent;
import com.app_afesox.ntfssox.events.notifications.NotificationEnvelope;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.forge.outbox.core.port.ForgeOutboxPublishMetadata;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {
                NotificationContentEventMapper.class,
                NotificationDeliveryEventMapper.class,
                NotificationMetaEventMapper.class,
                NotificationTemplateEventMapper.class
        })
public interface NotificationEventMapper {

    @Mapping(target = "content", source = "params")
    NotificationEvent asEvent(EmailVerifyPayload payload);

    @Mapping(target = "payload", source = "payload")
    @Mapping(target = "metadata", source = "metadata")
    NotificationEnvelope asEnvelope(EmailVerifyPayload payload,
                                    ForgeOutboxPublishMetadata metadata);

    @Mapping(target = "idempotencyId", source = "idempotencyId", qualifiedByName = "uuidToString")
    @Mapping(target = "createdAt", source = "createdAt")
    Metadata asMetadata(ForgeOutboxPublishMetadata metadata);

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
