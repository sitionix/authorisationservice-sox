package com.sitionix.athssox.application.outbox.handler;

import com.sitionix.athssox.domain.model.outbox.payload.handler.EventTypeHandler;
import org.springframework.stereotype.Service;

@Service
public class EmailVerifyHandler implements EventTypeHandler {

    @Override
    public <T> T getPayload(String payload) {
        return null;
    }
}
