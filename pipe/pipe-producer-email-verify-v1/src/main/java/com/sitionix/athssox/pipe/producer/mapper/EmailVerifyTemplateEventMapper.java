package com.sitionix.athssox.pipe.producer.mapper;

import com.app_afesox.ntfssox.events.notifications.NotificationTemplateDTO;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate;
import org.mapstruct.Mapper;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface EmailVerifyTemplateEventMapper {

    NotificationTemplateDTO asTemplate(NotificationTemplate template);
}
