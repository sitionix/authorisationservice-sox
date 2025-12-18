package com.sitionix.athssox.postgresql.repository;

import com.sitionix.athssox.domain.model.RefreshTokenRecord;
import com.sitionix.athssox.domain.repository.RefreshTokenRepository;
import com.sitionix.athssox.postgresql.entity.RefreshTokenEntity;
import com.sitionix.athssox.postgresql.jpa.RefreshTokenJpaRepository;
import com.sitionix.athssox.postgresql.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    public void save(final RefreshTokenRecord refreshTokenRecord) {
        final RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .tokenHash(refreshTokenRecord.getTokenHash())
                .user(this.userJpaRepository.getReferenceById(refreshTokenRecord.getUserId()))
                .expiresAt(refreshTokenRecord.getExpiresAt())
                .build();

        this.refreshTokenJpaRepository.save(entity);
    }
}
