package com.sitionix.athssox.postgresql.mapper.token;

import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.RefreshTokenStatus;
import com.sitionix.athssox.postgresql.entity.token.RefreshTokenStatusEntity;
import org.mapstruct.Mapper;

import static java.util.Objects.isNull;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface RefreshTokenStatusInfraMapper {

    default RefreshTokenStatus asStatus(final RefreshTokenStatusEntity statusEntity) {
        if (isNull(statusEntity)) {
            return null;
        }
        return RefreshTokenStatus.fromId(statusEntity.getId());
    }

    default RefreshTokenStatusEntity asStatusEntity(final RefreshTokenStatus status) {
        if (isNull(status)) {
            return null;
        }
        return RefreshTokenStatusEntity.builder()
                .id(status.getId())
                .description(status.getDescription())
                .build();
    }
}
