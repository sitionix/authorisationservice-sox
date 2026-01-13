package com.sitionix.athssox.domain.exception;

public class SessionNotActiveException extends RuntimeException {

    public SessionNotActiveException(final String message) {
        super(message);
    }
}
