package com.sitionix.athssox.domain.exception;

public class EmailVerificationTokenInvalidException extends RuntimeException {

    public EmailVerificationTokenInvalidException(final String message) {
        super(message);
    }
}
