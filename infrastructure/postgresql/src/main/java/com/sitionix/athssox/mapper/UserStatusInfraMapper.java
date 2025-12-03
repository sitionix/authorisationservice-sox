package com.sitionix.athssox.mapper;

import com.sitionix.athssox.config.MapstructComponent;
import com.sitionix.athssox.domain.RegisterUserDO;
import com.sitionix.athssox.domain.ResponseRegisterUser;
import com.sitionix.athssox.domain.UserStatus;
import com.sitionix.athssox.entity.UserStatusEntity;
import org.mapstruct.Mapper;

import static java.util.Objects.isNull;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface UserStatusInfraMapper {

    default UserStatus asStatus(final UserStatusEntity userStatusEntity) {
        if (isNull(userStatusEntity)) {
            return null;
        }
        return UserStatus.fromId(userStatusEntity.getId());
    }

    default UserStatusEntity asUserStatusEntity(final UserStatus userStatus) {
        if (isNull(userStatus)) {
            return null;
        }
        return UserStatusEntity.builder()
                .id(userStatus.getId())
                .description(userStatus.getDescription())
                .build();
    }
}
