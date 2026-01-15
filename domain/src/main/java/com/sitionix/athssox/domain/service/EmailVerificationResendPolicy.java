package com.sitionix.athssox.domain.service;

public interface EmailVerificationResendPolicy {

    boolean isResendAllowed(final Long userId);
}
