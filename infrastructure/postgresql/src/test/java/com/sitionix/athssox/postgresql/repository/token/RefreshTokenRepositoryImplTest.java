package com.sitionix.athssox.postgresql.repository.token;

import com.sitionix.athssox.domain.model.RefreshTokenRecord;
import com.sitionix.athssox.domain.repository.RefreshTokenRepository;
import com.sitionix.athssox.postgresql.entity.token.RefreshTokenEntity;
import com.sitionix.athssox.postgresql.jpa.token.RefreshTokenJpaRepository;
import com.sitionix.athssox.postgresql.mapper.token.RefreshTokenInfraMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryImplTest {

    private RefreshTokenRepository repository;

    @Mock
    private RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Mock
    private RefreshTokenInfraMapper refreshTokenInfraMapper;

    @BeforeEach
    void setUp() {
        this.repository = new RefreshTokenRepositoryImpl(
                this.refreshTokenJpaRepository,
                this.refreshTokenInfraMapper
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
                this.refreshTokenJpaRepository,
                this.refreshTokenInfraMapper
        );
    }

    @Test
    void givenRefreshTokenRecord_whenSave_thenVerify() {
        // Given
        final RefreshTokenRecord refreshTokenRecord = mock(RefreshTokenRecord.class);
        final RefreshTokenEntity entity = mock(RefreshTokenEntity.class);

        when(this.refreshTokenInfraMapper.asRefreshTokenEntity(refreshTokenRecord))
                .thenReturn(entity);

        // When
        this.repository.save(refreshTokenRecord);

        // Then
        verify(this.refreshTokenJpaRepository).save(entity);
    }
}