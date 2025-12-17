package com.sitionix.athssox.usecase;

import com.sitionix.athssox.domain.RegisterUserDO;
import com.sitionix.athssox.domain.ResponseRegisterUser;
import com.sitionix.athssox.domain.UserRole;
import com.sitionix.athssox.domain.UserStatus;
import com.sitionix.athssox.exception.InvalidPasswordException;
import com.sitionix.athssox.repository.UserRepository;
import com.sitionix.athssox.validator.PasswordPolicyValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserImplTest {

    private RegisterUser createUser;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        final PasswordEncoder passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        final PasswordPolicyValidator passwordPolicyValidator = new PasswordPolicyValidator();
        this.createUser = new RegisterUserImpl(this.userRepository, passwordEncoder, passwordPolicyValidator);
    }

    @AfterEach
    void tearDown (){
        verifyNoMoreInteractions(
                this.userRepository
        );
    }

    @Test
    void givenValidUser_whenCreateUser_thenHashPasswordSetPendingStatusAndReturnCreatedUser() {

        //given
        final String rawPassword = "StrongPassword123";

        final RegisterUserDO givenRegisterUserDO = RegisterUserDO.builder()
                .email("email@sitionix.com")
                .password(rawPassword)
                .role(UserRole.SITE_USER)
                .siteId(UUID.randomUUID())
                .build();

        final ResponseRegisterUser responseRegisterUser = ResponseRegisterUser.builder()
                .userId(1L)
                .status(UserStatus.PENDING_EMAIL_VERIFY)
                .build();

        when(this.userRepository.createUser(any(RegisterUserDO.class))).thenReturn(responseRegisterUser);

        //when
        final ResponseRegisterUser actual = this.createUser.execute(givenRegisterUserDO);

        //then
        assertThat(actual.getUserId()).isEqualTo(1L);
        assertThat(actual.getStatus()).isEqualTo(UserStatus.PENDING_EMAIL_VERIFY);
        assertThat(actual.getMessage()).isEqualTo("Registration successful. Please verify your email.");

        final ArgumentCaptor<RegisterUserDO> captor = ArgumentCaptor.forClass(RegisterUserDO.class);
        verify(this.userRepository, times(1)).createUser(captor.capture());
        final RegisterUserDO userToCreate = captor.getValue();
        assertThat(userToCreate.getStatus()).isEqualTo(UserStatus.PENDING_EMAIL_VERIFY);
        assertThat(userToCreate.getPassword()).isNotEqualTo(rawPassword);
        assertThat(Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().matches(rawPassword, userToCreate.getPassword())).isTrue();
    }

    @Test
    void givenInvalidPassword_whenCreateUser_thenThrowAndNotCallRepository() {
        //given
        final RegisterUserDO givenRegisterUserDO = RegisterUserDO.builder()
                .email("email@sitionix.com")
                .password("weak")
                .role(UserRole.SITE_USER)
                .siteId(UUID.randomUUID())
                .build();

        //when/then
        assertThatThrownBy(() -> this.createUser.execute(givenRegisterUserDO))
                .isInstanceOf(InvalidPasswordException.class);
    }

}
