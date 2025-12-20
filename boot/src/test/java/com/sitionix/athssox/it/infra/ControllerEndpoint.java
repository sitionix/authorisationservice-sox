package com.sitionix.athssox.it.infra;

import com.app_afesox.athssox.api_first.dto.ErrorDTO;
import com.app_afesox.athssox.api_first.dto.LoginRequestDTO;
import com.app_afesox.athssox.api_first.dto.LoginResponseDTO;
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
                        .withRequest("registerUserRequest.json")
                        .expectResponse("registerUserResponse.json")
                        .expectStatus(201)
        );
    }

    public static Endpoint<RegisterUserDTO, ErrorDTO> registerUserBadRequest() {
        return Endpoint.createContract(
                "/api/v1/users",
                HttpMethod.POST,
                RegisterUserDTO.class,
                ErrorDTO.class,
                (MockmvcDefault) context -> context.expectStatus(400)
        );
    }

    public static Endpoint<RegisterUserDTO, ErrorDTO> registerUserConflict() {
        return Endpoint.createContract(
                "/api/v1/users",
                HttpMethod.POST,
                RegisterUserDTO.class,
                ErrorDTO.class,
                (MockmvcDefault) context -> context.expectStatus(409)
        );
    }

    public static Endpoint<LoginRequestDTO, LoginResponseDTO> login() {
        return Endpoint.createContract(
                "/api/v1/auth/login",
                HttpMethod.POST,
                LoginRequestDTO.class,
                LoginResponseDTO.class,
                (MockmvcDefault) context -> context
                        .withRequest("loginRequest.json")
                        .expectResponse("loginResponse.json")
                        .expectStatus(200)
        );
    }

    public static Endpoint<LoginRequestDTO, ErrorDTO> loginUnauthorized() {
        return Endpoint.createContract(
                "/api/v1/auth/login",
                HttpMethod.POST,
                LoginRequestDTO.class,
                ErrorDTO.class,
                (MockmvcDefault) context -> context.expectStatus(401)
        );
    }

    public static Endpoint<LoginRequestDTO, ErrorDTO> loginForbidden() {
        return Endpoint.createContract(
                "/api/v1/auth/login",
                HttpMethod.POST,
                LoginRequestDTO.class,
                ErrorDTO.class,
                (MockmvcDefault) context -> context.expectStatus(403)
        );
    }

    public static Endpoint<LoginRequestDTO, ErrorDTO> loginBadRequest() {
        return Endpoint.createContract(
                "/api/v1/auth/login",
                HttpMethod.POST,
                LoginRequestDTO.class,
                ErrorDTO.class,
                (MockmvcDefault) context -> context.expectStatus(400)
        );
    }

}
