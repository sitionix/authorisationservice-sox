package com.sitionix.athssox.application.validator;

import com.sitionix.athssox.domain.exception.InvalidPasswordException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

import static java.util.Objects.isNull;

@Component
public class PasswordPolicyValidator {

    public static final String DEFAULT_ERROR_MESSAGE =
            "Password must be at least 8 characters and include upper/lowercase letters and digits.";

    private static final Pattern POLICY =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    public void validate(final String password) {
        if (isNull(password) || !POLICY.matcher(password).matches()) {
            throw new InvalidPasswordException(DEFAULT_ERROR_MESSAGE);
        }
    }
}

