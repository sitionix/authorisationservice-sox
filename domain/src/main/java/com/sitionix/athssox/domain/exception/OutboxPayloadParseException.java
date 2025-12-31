package com.sitionix.athssox.domain.exception;

public class OutboxPayloadParseException extends RuntimeException {

    public OutboxPayloadParseException(final String message) {
        super(message);
    }

    public OutboxPayloadParseException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
