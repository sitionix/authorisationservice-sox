package com.sitionix.athssox.postgresql.repository.token;

import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenRecord;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.postgresql.entity.token.EmailVerificationTokenEntity;
import com.sitionix.athssox.postgresql.jpa.token.EmailVerificationTokenJpaRepository;
import com.sitionix.athssox.postgresql.mapper.token.EmailVerificationTokenInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EmailVerificationTokenRepositoryImpl implements EmailVerificationTokenRepository {

    private final EmailVerificationTokenJpaRepository emailVerificationTokenJpaRepository;
    private final EmailVerificationTokenInfraMapper emailVerificationTokenInfraMapper;

    @Override
    public void save(final EmailVerificationTokenRecord tokenRecord) {
        final EmailVerificationTokenEntity entity = this.emailVerificationTokenInfraMapper.asEntity(tokenRecord);

        this.emailVerificationTokenJpaRepository.save(entity);
    }

    @Override
    public Optional<EmailVerificationTokenRecord> findById(final UUID tokenId) {
        return this.emailVerificationTokenJpaRepository.findById(tokenId)
                .map(this.emailVerificationTokenInfraMapper::asRecord);
    }

    @Override
    public Optional<EmailVerificationTokenRecord> findByHashedToken(final String hashedToken) {
        return this.emailVerificationTokenJpaRepository.findByTokenHash(hashedToken)
                .map(this.emailVerificationTokenInfraMapper::asRecord);
    }

    @Override
    public Optional<Instant> findLatestCreatedAtByUserId(final Long userId) {
        return this.emailVerificationTokenJpaRepository.findFirstByUser_IdOrderByCreatedAtDesc(userId)
                .map(EmailVerificationTokenEntity::getCreatedAt);
    }

    @Override
    public long countByUserIdAndCreatedAtAfter(final Long userId, final Instant createdAfter) {
        return this.emailVerificationTokenJpaRepository.countByUser_IdAndCreatedAtAfter(userId, createdAfter);
    }

    @Override
    public int deleteExpiredBefore(final Instant cutoff) {
        return this.emailVerificationTokenJpaRepository.deleteExpiredBefore(cutoff);
    }
}
