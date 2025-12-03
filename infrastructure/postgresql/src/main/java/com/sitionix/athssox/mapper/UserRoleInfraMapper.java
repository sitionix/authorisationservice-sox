package com.sitionix.athssox.mapper;

import com.sitionix.athssox.config.MapstructComponent;
import com.sitionix.athssox.domain.UserRole;
import com.sitionix.athssox.entity.GlobalRoleEntity;
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
