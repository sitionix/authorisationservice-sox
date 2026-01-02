package com.sitionix.athssox.postgresql.mapper.outbox;

import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.postgresql.entity.OutboxEventTypeEntity;
import com.sitionix.athssox.postgresql.entity.OutboxStatusEntity;
import org.mapstruct.Mapper;

import static java.util.Objects.isNull;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface OutboxStatusInfraMapper {

    default OutboxStatusEntity asOutboxStatusEntity(final OutboxStatus status) {
        if (isNull(status)) {
            return null;
        }
        return OutboxStatusEntity.builder()
                .id(status.getId())
                .description(status.getDescription())
                .build();
    }

    default OutboxStatus asEventType(final OutboxStatusEntity eventType) {
        return OutboxStatus.fromId(eventType.getId());
    }
}
