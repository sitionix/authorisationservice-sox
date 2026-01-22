package com.sitionix.athssox.domain.exception;

public class UserAlreadyVerifiedException extends RuntimeException {

    public UserAlreadyVerifiedException(final String message) {
        super(message);
    }
}
