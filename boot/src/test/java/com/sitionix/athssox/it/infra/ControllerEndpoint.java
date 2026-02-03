package com.sitionix.athssox.it.infra;

import com.app_afesox.athssox.api_first.dto.EmailVerificationDTO;
import com.app_afesox.athssox.api_first.dto.EmailVerificationResponseDTO;
import com.app_afesox.athssox.api_first.dto.ErrorDTO;
import com.app_afesox.athssox.api_first.dto.IssueEmailVerificationLinkResponseDTO;
import com.app_afesox.athssox.api_first.dto.LoginRequestDTO;
import com.app_afesox.athssox.api_first.dto.LoginResponseDTO;
import com.app_afesox.athssox.api_first.dto.RefreshAccessTokenRequestDTO;
import com.app_afesox.athssox.api_first.dto.RefreshAccessTokenResponseDTO;
import com.app_afesox.athssox.api_first.dto.RegisterUserDTO;
import com.app_afesox.athssox.api_first.dto.ResponseRegisterUserDTO;
import com.app_afesox.athssox.api_first.dto.JwksResponseDTO;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.domain.endpoint.mockmvc.MockmvcDefault;

public class ControllerEndpoint {

    private static final String INTERNAL_TOKEN =
            "Bearer eyJhbGciOiJIUzI1NiIsImtpZCI6Iml0IiwidHlwIjoiSldUIn0.eyJpc3MiOiJzaXRpb25peC1pbnRlcm5hbCIsInN1YiI6Iml0LXN0YXRpYyIsImF1ZCI6WyIqIl0sImlhdCI6MH0.mcHvkplVU4G7BXc8TjOJoQtdOJLlwfV3Ycg6hf-_riI";

    public static Endpoint<RegisterUserDTO, ResponseRegisterUserDTO> registerUser() {
        return Endpoint.createContract(
                "/api/v1/users",
                HttpMethod.POST,
                RegisterUserDTO.class,
                ResponseRegisterUserDTO.class,
                (MockmvcDefault) context -> context
                        .token(INTERNAL_TOKEN)
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
                (MockmvcDefault) context -> context
                        .token(INTERNAL_TOKEN)
                        .expectStatus(400)
        );
    }

    public static Endpoint<RegisterUserDTO, ErrorDTO> registerUserConflict() {
        return Endpoint.createContract(
                "/api/v1/users",
                HttpMethod.POST,
                RegisterUserDTO.class,
                ErrorDTO.class,
                (MockmvcDefault) context -> context
                        .token(INTERNAL_TOKEN)
                        .expectStatus(409)
        );
    }

    public static Endpoint<LoginRequestDTO, LoginResponseDTO> login() {
        return Endpoint.createContract(
                "/api/v1/auth/login",
                HttpMethod.POST,
                LoginRequestDTO.class,
                LoginResponseDTO.class,
                (MockmvcDefault) context -> context
                        .token(INTERNAL_TOKEN)
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
                (MockmvcDefault) context -> context
                        .token(INTERNAL_TOKEN)
                        .expectStatus(401)
        );
    }

    public static Endpoint<LoginRequestDTO, ErrorDTO> loginBadRequest() {
        return Endpoint.createContract(
                "/api/v1/auth/login",
                HttpMethod.POST,
                LoginRequestDTO.class,
                ErrorDTO.class,
                (MockmvcDefault) context -> context
                        .token(INTERNAL_TOKEN)
                        .expectStatus(400)
        );
    }


    public static Endpoint<EmailVerificationDTO, EmailVerificationResponseDTO> verifyEmailOk() {
        return Endpoint.createContract(
                "/api/v1/auth/email/verify",
                HttpMethod.POST,
                EmailVerificationDTO.class,
                EmailVerificationResponseDTO.class,
                (MockmvcDefault) context -> context
                        .token(INTERNAL_TOKEN)
                        .withRequest("verifyEmailRequest.json")
                        .expectResponse("verifyEmailResponse_ok.json")
                        .expectStatus(200)
        );
    }

    public static Endpoint<EmailVerificationDTO, EmailVerificationResponseDTO> verifyEmailAccepted() {
        return Endpoint.createContract(
                "/api/v1/auth/email/verify",
                HttpMethod.POST,
                EmailVerificationDTO.class,
                EmailVerificationResponseDTO.class,
                (MockmvcDefault) context -> context
                        .token(INTERNAL_TOKEN)
                        .withRequest("verifyEmailRequest.json")
                        .expectResponse("verifyEmailResponse_accepted.json")
                        .expectStatus(202)
        );
    }

    public static Endpoint<EmailVerificationDTO, ErrorDTO> verifyEmailBadRequest() {
        return Endpoint.createContract(
                "/api/v1/auth/email/verify",
                HttpMethod.POST,
                EmailVerificationDTO.class,
                ErrorDTO.class,
                (MockmvcDefault) context -> context
                        .token(INTERNAL_TOKEN)
                        .expectStatus(400)
        );
    }

    public static Endpoint<RefreshAccessTokenRequestDTO, RefreshAccessTokenResponseDTO> refreshAccessToken() {
        return Endpoint.createContract(
                "/api/v1/auth/refresh",
                HttpMethod.POST,
                RefreshAccessTokenRequestDTO.class,
                RefreshAccessTokenResponseDTO.class,
                (MockmvcDefault) context -> context
                        .token(INTERNAL_TOKEN)
                        .withRequest("refreshAccessTokenRequest.json")
                        .expectResponse("refreshAccessTokenResponse.json")
                        .expectStatus(200)
        );
    }

    public static Endpoint<Void, JwksResponseDTO> jwks() {
        return Endpoint.createContract(
                "/.well-known/jwks.json",
                HttpMethod.GET,
                Void.class,
                JwksResponseDTO.class,
                (MockmvcDefault) context -> context.expectStatus(200)
        );
    }

    public static Endpoint<Void, JwksResponseDTO> jwksAlias() {
        return Endpoint.createContract(
                "/oauth2/v1/keys",
                HttpMethod.GET,
                Void.class,
                JwksResponseDTO.class,
                (MockmvcDefault) context -> context.expectStatus(200)
        );
    }

    public static Endpoint<Void, IssueEmailVerificationLinkResponseDTO> issueEmailVerificationLink() {
        return Endpoint.createContract(
                "/api/v1/auth/emailVerificationTokens/{tokenId}:issueLink",
                HttpMethod.GET,
                Void.class,
                IssueEmailVerificationLinkResponseDTO.class,
                (MockmvcDefault) context -> context
                        .token(INTERNAL_TOKEN)
                        .expectStatus(200)
        );
    }

}
