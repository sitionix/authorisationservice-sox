package com.sitionix.athssox.config;

import com.app_afesox.athssox.api_first.dto.EmailVerificationDTO;
import com.app_afesox.athssox.api_first.dto.EmailVerificationResponseDTO;
import com.app_afesox.athssox.api_first.dto.ErrorDTO;
import com.app_afesox.athssox.api_first.dto.LoginRequestDTO;
import com.app_afesox.athssox.api_first.dto.LoginResponseDTO;
import com.app_afesox.athssox.api_first.dto.RegisterUserDTO;
import com.app_afesox.athssox.api_first.dto.ResponseRegisterUserDTO;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

@Configuration
@Profile("it")
class ApiFirstJacksonConfig {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer apiFirstJacksonMixins() {
        return builder -> builder.postConfigurer(mapper -> {
            mapper.addMixIn(LoginRequestDTO.class, LoginRequestDTOMixin.class);
            mapper.addMixIn(LoginResponseDTO.class, LoginResponseDTOMixin.class);
            mapper.addMixIn(RegisterUserDTO.class, RegisterUserDTOMixin.class);
            mapper.addMixIn(ResponseRegisterUserDTO.class, ResponseRegisterUserDTOMixin.class);
            mapper.addMixIn(EmailVerificationDTO.class, EmailVerificationDTOMixin.class);
            mapper.addMixIn(EmailVerificationResponseDTO.class, EmailVerificationResponseDTOMixin.class);
            mapper.addMixIn(ErrorDTO.class, ErrorDTOMixin.class);
        });
    }

    abstract static class LoginRequestDTOMixin {
        @JsonCreator
        LoginRequestDTOMixin(@JsonProperty("email") final String email,
                             @JsonProperty("password") final String password,
                             @JsonProperty("siteId") final UUID siteId,
                             @JsonProperty("sessionSourceId") final String sessionSourceId,
                             @JsonProperty("userAgent") final String userAgent) {
        }
    }

    abstract static class LoginResponseDTOMixin {
        @JsonCreator
        LoginResponseDTOMixin(@JsonProperty("accessToken") final String accessToken,
                              @JsonProperty("refreshToken") final String refreshToken,
                              @JsonProperty("tokenType") final String tokenType,
                              @JsonProperty("expiresIn") final Long expiresIn) {
        }
    }

    abstract static class RegisterUserDTOMixin {
        @JsonCreator
        RegisterUserDTOMixin(@JsonProperty("password") final String password,
                             @JsonProperty("email") final String email,
                             @JsonProperty("siteId") final UUID siteId,
                             @JsonProperty("role") final RegisterUserDTO.RoleEnum role) {
        }
    }

    abstract static class ResponseRegisterUserDTOMixin {
        @JsonCreator
        ResponseRegisterUserDTOMixin(@JsonProperty("message") final String message,
                                     @JsonProperty("userId") final Long userId,
                                     @JsonProperty("status") final ResponseRegisterUserDTO.StatusEnum status) {
        }
    }

    abstract static class EmailVerificationDTOMixin {
        @JsonCreator
        EmailVerificationDTOMixin(@JsonProperty("token") final String token,
                                  @JsonProperty("siteId") final UUID siteId) {
        }
    }

    abstract static class EmailVerificationResponseDTOMixin {
        @JsonCreator
        EmailVerificationResponseDTOMixin(@JsonProperty("message") final String message,
                                          @JsonProperty("status") final EmailVerificationResponseDTO.StatusEnum status) {
        }
    }

    abstract static class ErrorDTOMixin {
        @JsonCreator
        ErrorDTOMixin(@JsonProperty("code") final Integer code,
                      @JsonProperty("title") final String title,
                      @JsonProperty("details") final String details) {
        }
    }
}
