package com.sitionix.athssox.controller;

import com.app_afesox.athssox.api_first.dto.UserDTO;
import com.app_afesox.athssox.api_first.dto.UserResponseDTO;
import com.sitionix.athssox.domain.User;
import com.sitionix.athssox.mapper.UserApiMapper;
import com.sitionix.athssox.usecase.CreateUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UserControllerTest {

    @TestConfiguration
    static class TestContextConfiguration {
        @Bean
        public UserController userController(final CreateUser createUser,
                                             final UserApiMapper userApiMapper) {
            return new UserController(createUser, userApiMapper);
        }
    }

    @Autowired
    private UserController userController;

    @MockBean
    private CreateUser createUser;

    @MockBean
    private UserApiMapper userApiMapper;

    @AfterEach
    public void tearDown (){
        verifyNoMoreInteractions(
                this.createUser,
                this.userApiMapper);
    }

    @Test
    void givenUserDTO_whenCreate_thenReturnCreatedUser() {

        //given

        final UserDTO userDTO = Mockito.mock(UserDTO.class);
        final User user = Mockito.mock(User.class);
        final UserResponseDTO expectedBody = Mockito.mock(UserResponseDTO.class);

        final ResponseEntity<UserResponseDTO> expectedResponse = ResponseEntity
                .status(HttpStatus.CREATED)
                .body(expectedBody);

        when(this.userApiMapper.asUser(userDTO)).thenReturn(user);
        when(this.userApiMapper.asUserResponseDTO(user)).thenReturn(expectedBody);
        when(this.createUser.execute(user)).thenReturn(user);

        //when
        final ResponseEntity<UserResponseDTO> actualResponse = this.userController.createUser(userDTO);

        //then
        assertThat(expectedResponse).isEqualTo(actualResponse);

        //verify
        verify(this.userApiMapper, times(1)).asUser(userDTO);
        verify(this.userApiMapper, times(1)).asUserResponseDTO(user);
        verify(this.createUser, times(1)).execute(user);
    }

}