package com.sitionix.athssox.postgresql.mapper;

import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.postgresql.entity.GlobalRoleEntity;
import org.mapstruct.Mapper;

import static java.util.Objects.isNull;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface UserRoleInfraMapper {

    default GlobalRoleEntity asGlobalRoleEntity(final UserRole role) {
        if (isNull(role)) {
            return null;
        }
        return GlobalRoleEntity.builder()
                .id(role.getId())
                .description(role.getDescription())
                .build();

    }
}
