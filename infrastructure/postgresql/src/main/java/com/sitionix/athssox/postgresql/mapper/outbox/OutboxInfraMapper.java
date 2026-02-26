package com.sitionix.athssox.postgresql.mapper.outbox;

import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxEventEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {
                OutboxAggregateTypeInfraMapper.class,
                OutboxEventTypeInfraMapper.class,
                OutboxInitiatorTypeInfraMapper.class,
                OutboxStatusInfraMapper.class,
                OutboxPayloadJsonMapper.class
        })
public interface OutboxInfraMapper {

    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "payload", source = "payload", qualifiedByName = "toJson")
    @Mapping(target = "initiatorType", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    OutboxEventEntity toEntity(OutboxEvent<?> outboxEvent);


    @Mapping(target = "payload", source = "payload")
    @Mapping(target = "createdAt", source = "createdAt")
    OutboxEvent<Object> toOutboxEvent(OutboxEventEntity event);

    default Instant map(final LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(ZoneOffset.UTC).toInstant();
    }
}
