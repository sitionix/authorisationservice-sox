package com.sitionix.athssox.validator;

import com.sitionix.athssox.exception.InvalidPasswordException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyValidatorTest {

    private final PasswordPolicyValidator passwordPolicyValidator = new PasswordPolicyValidator();

    @Test
    void givenValidPassword_whenValidate_thenNoException() {
        assertThatNoException().isThrownBy(() -> this.passwordPolicyValidator.validate("StrongPassword123"));
    }

    @Test
    void givenPasswordWithoutDigit_whenValidate_thenThrow() {
        assertThatThrownBy(() -> this.passwordPolicyValidator.validate("StrongPassword"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage(PasswordPolicyValidator.DEFAULT_ERROR_MESSAGE);
    }

    @Test
    void givenTooShortPassword_whenValidate_thenThrow() {
        assertThatThrownBy(() -> this.passwordPolicyValidator.validate("A1b"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage(PasswordPolicyValidator.DEFAULT_ERROR_MESSAGE);
    }
}

