package com.sitionix.athssox.domain.exception;

public class EmailVerificationTokenNotFoundException extends RuntimeException {

    public EmailVerificationTokenNotFoundException(final String message) {
        super(message);
    }
}
