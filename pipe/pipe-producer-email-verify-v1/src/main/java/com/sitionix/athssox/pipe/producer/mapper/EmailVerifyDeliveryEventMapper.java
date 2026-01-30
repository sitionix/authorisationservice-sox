package com.sitionix.athssox.pipe.producer.mapper;

import com.app_afesox.ntfssox.events.notifications.DeliveryDTO;
import com.app_afesox.ntfssox.events.notifications.NotificationChannelDTO;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.VerifyChannel;
import org.mapstruct.Mapper;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface EmailVerifyDeliveryEventMapper {

    DeliveryDTO asDelivery(EmailVerifyPayload.Delivery delivery);

    NotificationChannelDTO asChannel(VerifyChannel channel);
}
