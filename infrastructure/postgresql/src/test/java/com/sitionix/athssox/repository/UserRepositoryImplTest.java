package com.sitionix.athssox.repository;

import com.sitionix.athssox.domain.RegisterUserDO;
import com.sitionix.athssox.domain.ResponseRegisterUser;
import com.sitionix.athssox.entity.UserEntity;
import com.sitionix.athssox.jpa.UserJpaRepository;
import com.sitionix.athssox.mapper.UserInfraMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    private UserRepository userRepository;

    @Mock
    public UserJpaRepository userJpaRepository;

    @Mock
    public UserInfraMapper userInfraMapper;

    @BeforeEach
    void setUp() {
        this.userRepository = new UserRepositoryImpl(
                this.userJpaRepository,
                this.userInfraMapper
        );
    }

    @BeforeEach
    void tearDown (){
        verifyNoMoreInteractions(
                this.userJpaRepository,
                this.userInfraMapper);
    }

    @Test
    void givenUser_thenCreateUser_thenReturnCreatedUser() {

        //given
        final RegisterUserDO givenRegisterUserDO = Mockito.mock(RegisterUserDO.class);
        final ResponseRegisterUser responseRegisterUser = Mockito.mock(ResponseRegisterUser.class);

        final UserEntity givenUserEntity = Mockito.mock(UserEntity.class);
        final UserEntity createdUserEntity = Mockito.mock(UserEntity.class);

        when(this.userInfraMapper.asUserEntity(givenRegisterUserDO)).thenReturn(givenUserEntity);
        when(this.userInfraMapper.asResponseRegisterUser(createdUserEntity)).thenReturn(responseRegisterUser);
        when(this.userJpaRepository.save(givenUserEntity)).thenReturn(createdUserEntity);

        //when

        final ResponseRegisterUser actual = this.userRepository.createUser(givenRegisterUserDO);

        //then

        assertThat(actual).isEqualTo(responseRegisterUser);

    }

}