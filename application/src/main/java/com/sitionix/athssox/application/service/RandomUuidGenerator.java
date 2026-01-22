package com.sitionix.athssox.application.service;

import com.sitionix.athssox.domain.service.UuidGenerator;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RandomUuidGenerator implements UuidGenerator {

    @Override
    public UUID generate() {
        return UUID.randomUUID();
    }
}
