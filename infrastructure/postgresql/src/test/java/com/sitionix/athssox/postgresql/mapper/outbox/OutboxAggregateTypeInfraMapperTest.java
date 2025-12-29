package com.sitionix.athssox.postgresql.mapper.outbox;

import com.sitionix.athssox.domain.model.outbox.OutboxAggregateType;
import com.sitionix.athssox.postgresql.entity.OutboxAggregateTypeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OutboxAggregateTypeInfraMapperTest {

    private OutboxAggregateTypeInfraMapper mapper;

    @BeforeEach
    void setUp() {
        this.mapper = new OutboxAggregateTypeInfraMapper() {
        };
    }

    @Test
    void givenNullAggregateType_whenAsOutboxAggregateTypeEntity_thenReturnNull() {
        //given
        final OutboxAggregateType given = null;

        //when
        final OutboxAggregateTypeEntity actual = this.mapper.asOutboxAggregateTypeEntity(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void givenAggregateType_whenAsOutboxAggregateTypeEntity_thenReturnEntity() {
        //given
        final OutboxAggregateType given = OutboxAggregateType.USER;
        final OutboxAggregateTypeEntity expected = this.getOutboxAggregateTypeEntity(1L, "USER");

        //when
        final OutboxAggregateTypeEntity actual = this.mapper.asOutboxAggregateTypeEntity(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private OutboxAggregateTypeEntity getOutboxAggregateTypeEntity(final Long id, final String description) {
        return OutboxAggregateTypeEntity.builder()
                .id(id)
                .description(description)
                .build();
    }
}
