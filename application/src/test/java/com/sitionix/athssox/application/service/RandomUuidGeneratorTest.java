package com.sitionix.athssox.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RandomUuidGeneratorTest {

    private RandomUuidGenerator generator;

    @BeforeEach
    void setUp() {
        this.generator = new RandomUuidGenerator();
    }

    @Test
    void givenGenerator_whenGenerate_thenReturnUniqueUuid() {
        //given

        //when
        final UUID first = this.generator.generate();
        final UUID second = this.generator.generate();

        //then
        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(first).isNotEqualTo(second);
    }
}
