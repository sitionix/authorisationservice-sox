package com.sitionix.athssox.postgresql.mapper.outbox;

import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.outbox.OutboxAggregateType;
import com.sitionix.athssox.postgresql.entity.OutboxAggregateTypeEntity;
import org.mapstruct.Mapper;

import static java.util.Objects.isNull;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface OutboxAggregateTypeInfraMapper {

    default OutboxAggregateTypeEntity asOutboxAggregateTypeEntity(final OutboxAggregateType aggregateType) {
        if (isNull(aggregateType)) {
            return null;
        }
        return OutboxAggregateTypeEntity.builder()
                .id(aggregateType.getId())
                .description(aggregateType.getDescription())
                .build();
    }
}
