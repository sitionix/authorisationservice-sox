package com.sitionix.athssox.domain.exception;

public class EmailVerificationResendNotAllowedException extends RuntimeException {

    public EmailVerificationResendNotAllowedException(final String message) {
        super(message);
    }
}
