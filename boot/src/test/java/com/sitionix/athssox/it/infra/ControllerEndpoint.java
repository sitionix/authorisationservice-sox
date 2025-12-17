package com.sitionix.athssox.it.infra;

import com.app_afesox.athssox.api_first.dto.ErrorDTO;
import com.app_afesox.athssox.api_first.dto.RegisterUserDTO;
import com.app_afesox.athssox.api_first.dto.ResponseRegisterUserDTO;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.domain.endpoint.mockmvc.MockmvcDefault;

public class ControllerEndpoint {

    public static Endpoint<RegisterUserDTO, ResponseRegisterUserDTO> registerUser() {
        return Endpoint.createContract(
                "/api/v1/users",
                HttpMethod.POST,
                RegisterUserDTO.class,
                ResponseRegisterUserDTO.class,
                (MockmvcDefault) context -> context
                        .request("registerUserRequest.json")
                        .response("registerUserResponse.json")
                        .status(201)
        );
    }

    public static Endpoint<RegisterUserDTO, ErrorDTO> registerUserBadRequest() {
        return Endpoint.createContract(
                "/api/v1/users",
                HttpMethod.POST,
                RegisterUserDTO.class,
                ErrorDTO.class,
                (MockmvcDefault) context -> context.status(400)
        );
    }

    public static Endpoint<RegisterUserDTO, ErrorDTO> registerUserConflict() {
        return Endpoint.createContract(
                "/api/v1/users",
                HttpMethod.POST,
                RegisterUserDTO.class,
                ErrorDTO.class,
                (MockmvcDefault) context -> context.status(409)
        );
    }
}
