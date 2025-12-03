package com.sitionix.athssox.mapper;

import com.sitionix.athssox.config.MapstructComponent;
import com.sitionix.athssox.domain.RegisterUserDO;
import com.sitionix.athssox.domain.ResponseRegisterUser;
import com.sitionix.athssox.entity.UserEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {UserRoleInfraMapper.class,
                UserStatusInfraMapper.class
        })
public interface UserInfraMapper {

    @Mapping(target = "passwordHash", source = "registerUserDO.password")
    @Mapping(target = "globalRole", source = "registerUserDO.role")
    UserEntity asUserEntity(final RegisterUserDO registerUserDO);

    @Mapping(target = "userId", source = "id")
    ResponseRegisterUser asResponseRegisterUser(final UserEntity userEntity);
}
