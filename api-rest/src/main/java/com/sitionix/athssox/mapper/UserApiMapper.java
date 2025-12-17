package com.sitionix.athssox.mapper;

import com.app_afesox.athssox.api_first.dto.RegisterUserDTO;
import com.app_afesox.athssox.api_first.dto.ResponseRegisterUserDTO;
import com.sitionix.athssox.config.MapstructComponent;
import com.sitionix.athssox.domain.RegisterUserDO;
import com.sitionix.athssox.domain.ResponseRegisterUser;
import com.sitionix.athssox.domain.UserRole;
import org.mapstruct.Mapper;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface UserApiMapper {

    RegisterUserDO asRegisterUser(final RegisterUserDTO src);

    ResponseRegisterUserDTO asResponseRegisterUserDTO(final ResponseRegisterUser src);

    default UserRole asUserRole(final RegisterUserDTO.RoleEnum role) {
        if (role == null) {
            return null;
        }
        return UserRole.valueOf(role.name());
    }
}
