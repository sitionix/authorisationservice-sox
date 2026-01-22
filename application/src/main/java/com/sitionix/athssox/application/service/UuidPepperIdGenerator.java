package com.sitionix.athssox.application.service;

import com.sitionix.athssox.domain.service.PepperIdGenerator;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UuidPepperIdGenerator implements PepperIdGenerator {

    @Override
    public UUID generate() {
        return UUID.randomUUID();
    }
}
