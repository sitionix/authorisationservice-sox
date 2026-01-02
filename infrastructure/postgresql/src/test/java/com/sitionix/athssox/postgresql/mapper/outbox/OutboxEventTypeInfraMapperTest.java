package com.sitionix.athssox.postgresql.mapper.outbox;

import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.postgresql.entity.OutboxEventTypeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OutboxEventTypeInfraMapperTest {

    private OutboxEventTypeInfraMapper mapper;

    @BeforeEach
    void setUp() {
        this.mapper = new OutboxEventTypeInfraMapper() {
        };
    }

    @Test
    void givenNullEventType_whenAsOutboxEventTypeEntity_thenReturnNull() {
        //given
        final OutboxEventType given = null;

        //when
        final OutboxEventTypeEntity actual = this.mapper.asOutboxEventTypeEntity(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void givenEventType_whenAsOutboxEventTypeEntity_thenReturnEntity() {
        //given
        final OutboxEventType given = OutboxEventType.EMAIL_VERIFY;
        final OutboxEventTypeEntity expected = this.getOutboxEventTypeEntity(1L, "EMAIL_VERIFY");

        //when
        final OutboxEventTypeEntity actual = this.mapper.asOutboxEventTypeEntity(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenEventTypeEntity_whenAsEventType_thenReturnEventType() {
        //given
        final OutboxEventTypeEntity given = this.getOutboxEventTypeEntity(1L, "EMAIL_VERIFY");
        final OutboxEventType expected = OutboxEventType.EMAIL_VERIFY;

        //when
        final OutboxEventType actual = this.mapper.asEventType(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private OutboxEventTypeEntity getOutboxEventTypeEntity(final Long id, final String description) {
        return OutboxEventTypeEntity.builder()
                .id(id)
                .description(description)
                .build();
    }
}
