package com.sitionix.athssox.controller;

import com.app_afesox.athssox.api_first.dto.RegisterUserDTO;
import com.app_afesox.athssox.api_first.dto.ResponseRegisterUserDTO;
import com.sitionix.athssox.domain.RegisterUserDO;
import com.sitionix.athssox.domain.ResponseRegisterUser;
import com.sitionix.athssox.mapper.UserApiMapper;
import com.sitionix.athssox.usecase.RegisterUser;
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
class RegisterUserDOControllerTest {

    private UserController userController;

    @Mock
    private UserApiMapper userApiMapper;

    @Mock
    private RegisterUser registerUser;

    @BeforeEach
    void setUp() {
        this.userController = new UserController(
                this.userApiMapper,
                this.registerUser);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
                this.userApiMapper,
                this.registerUser);
    }

    @Test
    void givenRegisterUserDTO_whenRegisterUser_thenReturnResponseEntity() {
        //given

        final RegisterUserDTO given = mock(RegisterUserDTO.class);
        final RegisterUserDO mappedUser = mock(RegisterUserDO.class);

        final ResponseRegisterUser mappedResponse = mock(ResponseRegisterUser.class);
        final ResponseRegisterUserDTO expected = mock(ResponseRegisterUserDTO.class);

        when(this.userApiMapper.asRegisterUser(given)).thenReturn(mappedUser);
        when(this.registerUser.execute(mappedUser)).thenReturn(mappedResponse);
        when(this.userApiMapper.asResponseRegisterUserDTO(mappedResponse)).thenReturn(expected);

        //when
        final ResponseEntity<ResponseRegisterUserDTO> actual = this.userController.registerUser(given);

        //then
        assertThat(actual.getBody()).isEqualTo(expected);
    }
}