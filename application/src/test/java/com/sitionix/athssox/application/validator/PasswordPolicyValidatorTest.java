package com.sitionix.athssox.application.validator;

import com.sitionix.athssox.domain.exception.InvalidPasswordException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyValidatorTest {

    private final PasswordPolicyValidator passwordPolicyValidator = new PasswordPolicyValidator();

    @Test
    void given_valid_password_when_validate_then_no_exception() {
        assertThatNoException().isThrownBy(() -> this.passwordPolicyValidator.validate("StrongPassword123"));
    }

    @Test
    void given_password_without_digit_when_validate_then_throw() {
        assertThatThrownBy(() -> this.passwordPolicyValidator.validate("StrongPassword"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage(PasswordPolicyValidator.DEFAULT_ERROR_MESSAGE);
    }

    @Test
    void given_too_short_password_when_validate_then_throw() {
        assertThatThrownBy(() -> this.passwordPolicyValidator.validate("A1b"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage(PasswordPolicyValidator.DEFAULT_ERROR_MESSAGE);
    }
}

