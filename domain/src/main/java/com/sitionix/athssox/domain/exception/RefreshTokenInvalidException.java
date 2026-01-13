package com.sitionix.athssox.domain.exception;

public class RefreshTokenInvalidException extends RuntimeException {

    public RefreshTokenInvalidException(final String message) {
        super(message);
    }
}
