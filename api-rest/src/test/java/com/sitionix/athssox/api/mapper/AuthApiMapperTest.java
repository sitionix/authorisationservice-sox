package com.sitionix.athssox.api.mapper;

import com.app_afesox.athssox.api_first.dto.LoginRequestDTO;
import com.app_afesox.athssox.api_first.dto.LoginResponseDTO;
import com.sitionix.athssox.domain.model.LoginRequest;
import com.sitionix.athssox.domain.model.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthApiMapperTest {

    private AuthApiMapper authApiMapper;

    @BeforeEach
    void setUp() {
        this.authApiMapper = new AuthApiMapperImpl();
    }

    @Test
    void given_login_request_dto_when_as_login_request_then_return_login_request() {
        //given
        final UUID siteId = UUID.randomUUID();
        final LoginRequestDTO given = this.getLoginRequestDTO(siteId);
        final LoginRequest expected = this.getLoginRequest(siteId);

        //when
        final LoginRequest actual = this.authApiMapper.asLoginRequest(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_login_response_when_as_login_response_dto_then_return_login_response_dto() {
        //given
        final LoginResponse given = this.getLoginResponse();
        final LoginResponseDTO expected = this.getLoginResponseDTO();

        //when
        final LoginResponseDTO actual = this.authApiMapper.asLoginResponseDTO(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private LoginRequestDTO getLoginRequestDTO(final UUID siteId) {
        return LoginRequestDTO.builder()
                .email("user@sitionix.com")
                .password("StrongPassword123")
                .siteId(siteId)
                .sessionSourceId("device-123")
                .userAgent("Mozilla/5.0")
                .build();
    }

    private LoginRequest getLoginRequest(final UUID siteId) {
        return LoginRequest.builder()
                .email("user@sitionix.com")
                .password("StrongPassword123")
                .siteId(siteId)
                .sessionSourceId("device-123")
                .userAgent("Mozilla/5.0")
                .build();
    }

    private LoginResponseDTO getLoginResponseDTO() {
        return LoginResponseDTO.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .expiresIn(3600L)
                .tokenType("Bearer")
                .build();
    }

    private LoginResponse getLoginResponse() {
        return LoginResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .expiresIn(3600L)
                .tokenType("Bearer")
                .build();
    }
}
