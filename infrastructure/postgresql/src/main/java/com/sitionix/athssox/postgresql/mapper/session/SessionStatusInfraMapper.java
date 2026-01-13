package com.sitionix.athssox.postgresql.mapper.session;

import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.SessionStatus;
import com.sitionix.athssox.postgresql.entity.session.SessionStatusEntity;
import org.mapstruct.Mapper;

import static java.util.Objects.isNull;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface SessionStatusInfraMapper {

    default SessionStatus asStatus(final SessionStatusEntity sessionStatusEntity) {
        if (isNull(sessionStatusEntity)) {
            return null;
        }
        return SessionStatus.fromId(sessionStatusEntity.getId());
    }

    default SessionStatusEntity asSessionStatusEntity(final SessionStatus sessionStatus) {
        if (isNull(sessionStatus)) {
            return null;
        }
        return SessionStatusEntity.builder()
                .id(sessionStatus.getId())
                .description(sessionStatus.getDescription())
                .build();
    }
}
