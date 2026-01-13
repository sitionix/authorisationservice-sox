package com.sitionix.athssox.domain.exception;

public class SessionMismatchException extends RuntimeException {

    public SessionMismatchException(final String message) {
        super(message);
    }
}
