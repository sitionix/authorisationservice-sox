package com.sitionix.athssox.api.controller;

import com.app_afesox.athssox.api_first.dto.RegisterUserDTO;
import com.app_afesox.athssox.api_first.dto.ResponseRegisterUserDTO;
import com.sitionix.athssox.api.mapper.UserApiMapper;
import com.sitionix.athssox.domain.model.RegisterUserDO;
import com.sitionix.athssox.domain.model.ResponseRegisterUser;
import com.sitionix.athssox.domain.usecase.RegisterUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private UserController userController;

    @Mock
    private UserApiMapper userApiMapper;

    @Mock
    private RegisterUser registerUser;

    @BeforeEach
    void setUp() {
        this.userController = new UserController(this.userApiMapper,
                this.registerUser);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.userApiMapper,
                this.registerUser);
    }

    @Test
    void givenRegisterUserDto_whenRegisterUser_thenReturnResponseRegisterUserDto() {
        //given
        final RegisterUserDTO given = mock(RegisterUserDTO.class);
        final RegisterUserDO registerUserDO = mock(RegisterUserDO.class);
        final ResponseRegisterUserDTO expected = mock(ResponseRegisterUserDTO.class);
        final ResponseRegisterUser responseRegisterUser = mock(ResponseRegisterUser.class);

        when(this.userApiMapper.asRegisterUser(given))
                .thenReturn(registerUserDO);
        when(this.registerUser.execute(registerUserDO))
                .thenReturn(responseRegisterUser);
        when(this.userApiMapper.asResponseRegisterUserDTO(responseRegisterUser))
                .thenReturn(expected);

        //when
        final ResponseEntity<ResponseRegisterUserDTO> actual =
                this.userController.registerUser(given);

        //then
        assertThat(actual).isEqualTo(ResponseEntity
                .status(HttpStatus.CREATED)
                .body(expected));
        verify(this.userApiMapper)
                .asRegisterUser(given);
        verify(this.registerUser)
                .execute(registerUserDO);
        verify(this.userApiMapper)
                .asResponseRegisterUserDTO(responseRegisterUser);
    }
}
