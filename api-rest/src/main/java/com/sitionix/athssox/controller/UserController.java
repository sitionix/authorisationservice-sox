package com.sitionix.athssox.controller;

import com.app_afesox.athssox.api_first.api.UserApi;
import com.app_afesox.athssox.api_first.dto.UserDTO;
import com.app_afesox.athssox.api_first.dto.UserResponseDTO;
import com.sitionix.athssox.domain.User;
import com.sitionix.athssox.mapper.UserApiMapper;
import com.sitionix.athssox.usecase.CreateUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final CreateUser createUser;

    private final UserApiMapper userDtoMapper;

    @Override
    public ResponseEntity<UserResponseDTO> createUser(@Valid UserDTO userDTO) {
        final User user = this.userDtoMapper.asUser(userDTO);
        final User createdUser = this.createUser.execute(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.userDtoMapper.asUserResponseDTO(createdUser));
    }
}
