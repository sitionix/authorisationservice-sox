package com.sitionix.athssox.postgresql.mapper.outbox;

import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxEventTypeEntity;
import org.mapstruct.Mapper;

import static java.util.Objects.isNull;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface OutboxEventTypeInfraMapper {

    default OutboxEventTypeEntity asOutboxEventTypeEntity(final OutboxEventType eventType) {
        if (isNull(eventType)) {
            return null;
        }
        return OutboxEventTypeEntity.builder()
                .id(eventType.getId())
                .description(eventType.getDescription())
                .build();
    }

    default OutboxEventType asEventType(final OutboxEventTypeEntity eventType) {
        return OutboxEventType.fromId(eventType.getId());
    }
}
