package com.sitionix.athssox.domain.exception;

public class EmailVerificationTokenExpiredException extends RuntimeException {

    public EmailVerificationTokenExpiredException(final String message) {
        super(message);
    }
}
