package com.sitionix.athssox.pipe.producer.mapper;

import com.app_afesox.ntfssox.events.notifications.contents.EmailVerificationContentDTO;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface NotificationContentEventMapper {

    @Mapping(target = "verificationTokenId", source = "emailVerificationTokenId")
    EmailVerificationContentDTO asContent(EmailVerifyPayload.Params params);

    default String toString(final UUID value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}
