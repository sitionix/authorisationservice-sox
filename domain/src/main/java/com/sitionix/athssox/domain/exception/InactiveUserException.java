package com.sitionix.athssox.domain.exception;

public class InactiveUserException extends RuntimeException {

    public InactiveUserException(final String message) {
        super(message);
    }
}
