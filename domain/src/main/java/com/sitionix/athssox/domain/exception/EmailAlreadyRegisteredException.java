package com.sitionix.athssox.domain.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public EmailAlreadyRegisteredException(final String message) {
        super(message);
    }
}

