package com.sitionix.athssox.domain.service;

import java.util.UUID;

public interface EmailVerificationTokenIdGenerator {

    UUID generate();
}
