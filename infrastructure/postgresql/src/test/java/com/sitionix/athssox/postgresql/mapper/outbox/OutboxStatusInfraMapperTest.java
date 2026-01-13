package com.sitionix.athssox.postgresql.mapper.outbox;

import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxStatusEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OutboxStatusInfraMapperTest {

    private OutboxStatusInfraMapper mapper;

    @BeforeEach
    void setUp() {
        this.mapper = new OutboxStatusInfraMapper() {
        };
    }

    @Test
    void givenNullStatus_whenAsOutboxStatusEntity_thenReturnNull() {
        //given
        final OutboxStatus given = null;

        //when
        final OutboxStatusEntity actual = this.mapper.asOutboxStatusEntity(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void givenStatus_whenAsOutboxStatusEntity_thenReturnEntity() {
        //given
        final OutboxStatus given = OutboxStatus.PENDING;
        final OutboxStatusEntity expected = this.getOutboxStatusEntity(1L, "PENDING");

        //when
        final OutboxStatusEntity actual = this.mapper.asOutboxStatusEntity(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenStatusEntity_whenAsEventType_thenReturnStatus() {
        //given
        final OutboxStatusEntity given = this.getOutboxStatusEntity(1L, "PENDING");
        final OutboxStatus expected = OutboxStatus.PENDING;

        //when
        final OutboxStatus actual = this.mapper.asEventType(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private OutboxStatusEntity getOutboxStatusEntity(final Long id, final String description) {
        return OutboxStatusEntity.builder()
                .id(id)
                .description(description)
                .build();
    }
}
