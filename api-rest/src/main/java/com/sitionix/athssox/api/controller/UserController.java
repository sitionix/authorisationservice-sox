package com.sitionix.athssox.api.controller;

import com.app_afesox.athssox.api_first.api.UserApi;
import com.app_afesox.athssox.api_first.dto.RegisterUserDTO;
import com.app_afesox.athssox.api_first.dto.ResponseRegisterUserDTO;
import com.sitionix.athssox.domain.model.RegisterUserDO;
import com.sitionix.athssox.domain.model.ResponseRegisterUser;
import com.sitionix.athssox.api.mapper.UserApiMapper;
import com.sitionix.athssox.domain.usecase.RegisterUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserApiMapper userDtoMapper;

    private final RegisterUser registerUser;

    @Override
    public ResponseEntity<ResponseRegisterUserDTO> registerUser(@Valid final RegisterUserDTO registerUserDTO) {
        log.info("Received request to register user for email: {}", registerUserDTO.getEmail());

        final RegisterUserDO user = this.userDtoMapper.asRegisterUser(registerUserDTO);
        final ResponseRegisterUser responseRegisterUser = this.registerUser.execute(user);

        log.info("User registration completed: {}", responseRegisterUser);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(this.userDtoMapper.asResponseRegisterUserDTO(responseRegisterUser));
    }
}
