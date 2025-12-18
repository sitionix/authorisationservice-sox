package com.sitionix.athssox.api.mapper;

import com.app_afesox.athssox.api_first.dto.LoginRequestDTO;
import com.app_afesox.athssox.api_first.dto.LoginResponseDTO;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.LoginRequest;
import com.sitionix.athssox.domain.model.LoginResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface AuthApiMapper {

    LoginRequest asLoginRequest(final LoginRequestDTO loginRequestDTO);

    LoginResponseDTO asLoginResponseDTO(final LoginResponse loginResponse);
}
