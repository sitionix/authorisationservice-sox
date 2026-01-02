package com.sitionix.athssox.postgresql.mapper.outbox;

import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.outbox.payload.InitiatorType;
import com.sitionix.athssox.postgresql.entity.OutboxInitiatorTypeEntity;
import org.mapstruct.Mapper;

import static java.util.Objects.isNull;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface OutboxInitiatorTypeInfraMapper {

    default InitiatorType asInitiatorType(final OutboxInitiatorTypeEntity initiatorType) {
        if (isNull(initiatorType)) {
            return null;
        }
        return InitiatorType.fromId(initiatorType.getId());
    }
}
