package com.sitionix.athssox.pipe.producer.mapper;

import com.app_afesox.ntfssox.events.notifications.MetaDTO;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface EmailVerifyMetaEventMapper {

    MetaDTO asMeta(EmailVerifyPayload.Meta meta);

    default String toDateTime(final Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.toString();
    }

    default String toString(final UUID value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}
