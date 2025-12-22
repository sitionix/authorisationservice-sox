package com.sitionix.athssox.postgresql.mapper.outbox;

import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.postgresql.entity.OutboxEventEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface OutboxInfraMapper {

    OutboxEventEntity toEntity(OutboxEvent<?> outboxEvent);
}
