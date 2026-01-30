package com.sitionix.athssox.it.infra;

import com.app_afesox.ntfssox.events.kafka.AvroRecordDeserializer;
import com.app_afesox.ntfssox.events.notifications.NotificationEvent;
import com.app_afesox.ntfssox.events.notifications.NotificationEnvelope;
import com.app_afesox.events.Metadata;
import com.sitionix.forgeit.kafka.api.KafkaContract;

public class OutboxKafkaContracts {

    public static final KafkaContract<NotificationEnvelope> EMAIL_VERIFY_EVENT_ENVELOPE_KAFKA_CONTRACT =
            KafkaContract.consumerContract()
                    .topicFromProperty("forge-it.kafka.notifications-topic")
                    .groupId("forge-it-group")
                    .payloadDeserializer(AvroRecordDeserializer.class)
                    .defaultEnvelope(NotificationEnvelope.class)
                    .defaultExpectedPayload(NotificationEvent.class, "defaultUserCreatedEvent.json")
                    .defaultMetadata(Metadata.class, "defaultUserCreatedMetadata.json")
                    .build();
}
