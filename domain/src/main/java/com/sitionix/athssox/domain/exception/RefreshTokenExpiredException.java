package com.sitionix.athssox.domain.exception;

public class RefreshTokenExpiredException extends RuntimeException {

    public RefreshTokenExpiredException(final String message) {
        super(message);
    }
}
