package com.sitionix.athssox.api.validation;

public class RequestBodyTooLargeException extends RuntimeException {

    public RequestBodyTooLargeException(final String message) {
        super(message);
    }
}
