package com.sitionix.athssox.application.service;

import com.sitionix.athssox.domain.repository.RefreshTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRevocationServiceTest {

    private RefreshTokenRevocationService refreshTokenRevocationService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        this.refreshTokenRevocationService = new RefreshTokenRevocationService(this.refreshTokenRepository);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.refreshTokenRepository);
    }

    @Test
    void givenNullTokenId_whenRevokeIfActive_thenReturnFalse() {
        //given
        final Long tokenId = null;
        final Instant now = this.getNow();
        final String reason = this.getReason();

        //when
        final boolean actual = this.refreshTokenRevocationService.revokeIfActive(tokenId, now, reason);

        //then
        assertThat(actual).isFalse();
    }

    @Test
    void givenActiveToken_whenRevokeIfActive_thenReturnTrue() {
        //given
        final Long tokenId = this.getTokenId();
        final Instant now = this.getNow();
        final String reason = this.getReason();

        when(this.refreshTokenRepository.revokeIfActive(tokenId, now, reason))
                .thenReturn(true);

        //when
        final boolean actual = this.refreshTokenRevocationService.revokeIfActive(tokenId, now, reason);

        //then
        assertThat(actual).isTrue();
        verify(this.refreshTokenRepository)
                .revokeIfActive(tokenId, now, reason);
    }

    @Test
    void givenInactiveToken_whenRevokeIfActive_thenReturnFalse() {
        //given
        final Long tokenId = this.getTokenId();
        final Instant now = this.getNow();
        final String reason = this.getReason();

        when(this.refreshTokenRepository.revokeIfActive(tokenId, now, reason))
                .thenReturn(false);

        //when
        final boolean actual = this.refreshTokenRevocationService.revokeIfActive(tokenId, now, reason);

        //then
        assertThat(actual).isFalse();
        verify(this.refreshTokenRepository)
                .revokeIfActive(tokenId, now, reason);
    }

    private Long getTokenId() {
        return 10L;
    }

    private Instant getNow() {
        return Instant.parse("2024-01-10T10:15:30Z");
    }

    private String getReason() {
        return "SUSPICIOUS";
    }
}
