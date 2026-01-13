package com.sitionix.athssox.it;

import com.sitionix.athssox.domain.model.RefreshTokenRecord;
import com.sitionix.athssox.domain.repository.RefreshTokenRepository;
import com.sitionix.athssox.domain.service.TokenHasher;
import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@IntegrationTest
class AuthRefreshMissingSessionIT {

    @Autowired
    private TestManager testManager;

    @MockBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockBean
    private TokenHasher tokenHasher;

    @Test
    @DisplayName("Should reject refresh when session does not exist")
    void givenRefreshTokenWithoutSession_whenRefreshAccessToken_thenForbidden() {
        //given
        final RefreshTokenRecord tokenRecord = this.getRefreshTokenRecordWithoutSession();

        when(this.tokenHasher.hash(anyString()))
                .thenReturn("hashed-token");
        when(this.refreshTokenRepository.findByTokenHash("hashed-token"))
                .thenReturn(Optional.of(tokenRecord));

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json")
                .expectResponse("refreshAccessTokenResponse_forbidden_session.json")
                .expectStatus(HttpStatus.FORBIDDEN)
                .assertAndCreate();

        //then
        verify(this.tokenHasher).hash("refresh-token-valid");
        verify(this.refreshTokenRepository).findByTokenHash("hashed-token");
        verifyNoMoreInteractions(this.refreshTokenRepository, this.tokenHasher);
    }

    private RefreshTokenRecord getRefreshTokenRecordWithoutSession() {
        return RefreshTokenRecord.builder()
                .tokenHash("hashed-token")
                .expiresAt(Instant.parse("2099-01-02T00:00:00Z"))
                .createdAt(Instant.parse("2099-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2099-01-01T00:00:00Z"))
                .build();
    }
}
