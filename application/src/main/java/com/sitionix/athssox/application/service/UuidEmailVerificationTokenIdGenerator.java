package com.sitionix.athssox.application.service;

import com.sitionix.athssox.domain.service.EmailVerificationTokenIdGenerator;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UuidEmailVerificationTokenIdGenerator implements EmailVerificationTokenIdGenerator {

    @Override
    public UUID generate() {
        return UUID.randomUUID();
    }
}
