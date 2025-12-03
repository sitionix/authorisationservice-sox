package com.sitionix.athssox.usecase;

import com.sitionix.athssox.domain.RegisterUserDO;
import com.sitionix.athssox.domain.ResponseRegisterUser;
import com.sitionix.athssox.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserImplTest {

    private RegisterUser createUser;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        this.createUser = new RegisterUserImpl(this.userRepository);
    }

    @AfterEach
    void tearDown (){
        verifyNoMoreInteractions(
                this.userRepository
        );
    }

    @Test
    void givenUser_whenCreateUser_thenReturnCreatedUser() {

        //given
        final RegisterUserDO givenRegisterUserDO = Mockito.mock(RegisterUserDO.class);
        final ResponseRegisterUser responseRegisterUser = Mockito.mock(ResponseRegisterUser.class);

        when(this.userRepository.createUser(givenRegisterUserDO)).thenReturn(responseRegisterUser);

        //when
        final ResponseRegisterUser actual = this.createUser.execute(givenRegisterUserDO);

        //then
        assertThat(actual).isEqualTo(responseRegisterUser);
    }

}