package com.sitionix.athssox.api.mapper;

import com.app_afesox.athssox.api_first.dto.RegisterUserDTO;
import com.app_afesox.athssox.api_first.dto.ResponseRegisterUserDTO;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.RegisterUserDO;
import com.sitionix.athssox.domain.model.ResponseRegisterUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface UserApiMapper {

    @Mapping(target = "status", constant = "PENDING_EMAIL_VERIFY")
    RegisterUserDO asRegisterUser(final RegisterUserDTO src);

    ResponseRegisterUserDTO asResponseRegisterUserDTO(final ResponseRegisterUser src);
}
