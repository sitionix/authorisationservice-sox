package com.sitionix.athssox.postgresql.mapper;

import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.RegisterUserDO;
import com.sitionix.athssox.domain.model.ResponseRegisterUser;
import com.sitionix.athssox.postgresql.entity.UserEntity;
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
