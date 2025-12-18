package com.sitionix.athssox.api.controller;

import com.app_afesox.athssox.api_first.dto.LoginRequestDTO;
import com.app_afesox.athssox.api_first.dto.LoginResponseDTO;
import com.sitionix.athssox.api.mapper.AuthApiMapper;
import com.sitionix.athssox.domain.model.LoginRequest;
import com.sitionix.athssox.domain.model.LoginResponse;
import com.sitionix.athssox.domain.usecase.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private AuthController authController;

    @Mock
    private AuthApiMapper authApiMapper;

    @Mock
    private LoginUser loginUser;

    @BeforeEach
    void setUp() {
        this.authController = new AuthController(this.authApiMapper,
                this.loginUser);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.authApiMapper,
                this.loginUser);
    }

    @Test
    void givenLoginRequestDTO_whenLogin_thenReturnLoginResponseDTO() {
        //given
        final LoginRequestDTO given = mock(LoginRequestDTO.class);
        final LoginRequest loginRequest = mock(LoginRequest.class);
        final LoginResponse loginResponse = mock(LoginResponse.class);
        final LoginResponseDTO expected = mock(LoginResponseDTO.class);

        when(this.authApiMapper.asLoginRequest(given))
                .thenReturn(loginRequest);
        when(this.loginUser.execute(loginRequest))
                .thenReturn(loginResponse);
        when(this.authApiMapper.asLoginResponseDTO(loginResponse))
                .thenReturn(expected);

        //when
        final ResponseEntity<LoginResponseDTO> actual = this.authController.login(given);

        //then
        assertThat(actual).isEqualTo(ResponseEntity.ok(expected));
    }
}
