package com.sitionix.athssox.pipe.producer.emailverify.v1;

import com.sitionix.athssox.domain.service.EmailVerifyPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailVerifyPublisherV1 implements EmailVerifyPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailVerifyPublisherV1.class);

    @Override
    public void publish() {
        LOGGER.info("Email verify publish invoked");
    }
}
