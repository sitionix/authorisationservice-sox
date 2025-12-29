package com.sitionix.athssox.postgresql.mapper.outbox;

import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.postgresql.entity.OutboxEventEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {
                OutboxAggregateTypeInfraMapper.class,
                OutboxEventTypeInfraMapper.class,
                OutboxStatusInfraMapper.class,
                OutboxPayloadJsonMapper.class
        })
public interface OutboxInfraMapper {

    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "payload", source = "payload", qualifiedByName = "toJson")
    OutboxEventEntity toEntity(OutboxEvent<?> outboxEvent);
}
