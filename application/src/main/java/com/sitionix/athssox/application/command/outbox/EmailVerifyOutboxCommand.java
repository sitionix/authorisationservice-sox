package com.sitionix.athssox.application.command.outbox;

import com.sitionix.athssox.domain.command.OutboxCommand;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerifyOutboxCommand implements OutboxCommand<EmailVerifyPayload> {

    private final OutboxEventRepository outboxEventRepository;

    @Override
    public void execute(final OutboxEvent<EmailVerifyPayload> payload) {
        this.outboxEventRepository.create(payload);
    }
}
