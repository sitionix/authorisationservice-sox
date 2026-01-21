package com.sitionix.athssox.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class Sha256TokenHasherTest {

    private Sha256TokenHasher sha256TokenHasher;

    @BeforeEach
    void setUp() {
        this.sha256TokenHasher = new Sha256TokenHasher();
    }

    @Test
    void given_token_when_hash_then_return_sha256_hash() {
        //given
        final String given = this.getToken();
        final String expected = "3c469e9d6c5875d37a43f353d4f88e61fcf812c66eee3457465a40b0da4153e0";

        //when
        final String actual = this.sha256TokenHasher.hash(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private String getToken() {
        return "token";
    }
}
