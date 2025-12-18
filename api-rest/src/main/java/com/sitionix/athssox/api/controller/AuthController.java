package com.sitionix.athssox.api.controller;

import com.app_afesox.athssox.api_first.api.AuthApi;
import com.app_afesox.athssox.api_first.dto.LoginRequestDTO;
import com.app_afesox.athssox.api_first.dto.LoginResponseDTO;
import com.sitionix.athssox.api.mapper.AuthApiMapper;
import com.sitionix.athssox.domain.model.LoginRequest;
import com.sitionix.athssox.domain.model.LoginResponse;
import com.sitionix.athssox.domain.usecase.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthApiMapper authApiMapper;
    private final LoginUser loginUser;

    @Override
    public ResponseEntity<LoginResponseDTO> login(@Valid final LoginRequestDTO loginRequestDTO) {
        log.info("Received login request for email: {}", loginRequestDTO.getEmail());

        final LoginRequest loginRequest = this.authApiMapper.asLoginRequest(loginRequestDTO);
        final LoginResponse loginResponse = this.loginUser.execute(loginRequest);

        log.info("Login completed for email: {}", loginRequestDTO.getEmail());
        return ResponseEntity.ok(this.authApiMapper.asLoginResponseDTO(loginResponse));
    }
}
