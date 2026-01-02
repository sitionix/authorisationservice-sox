package com.sitionix.athssox.postgresql.repository;

import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenRecord;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.postgresql.entity.EmailVerificationTokenEntity;
import com.sitionix.athssox.postgresql.jpa.EmailVerificationTokenJpaRepository;
import com.sitionix.athssox.postgresql.mapper.EmailVerificationTokenInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmailVerificationTokenRepositoryImpl implements EmailVerificationTokenRepository {

    private final EmailVerificationTokenJpaRepository emailVerificationTokenJpaRepository;
    private final EmailVerificationTokenInfraMapper emailVerificationTokenInfraMapper;

    @Override
    public void save(final EmailVerificationTokenRecord record) {
        final EmailVerificationTokenEntity entity = this.emailVerificationTokenInfraMapper.asEntity(record);

        this.emailVerificationTokenJpaRepository.save(entity);
    }

    @Override
    public Optional<EmailVerificationTokenRecord> findByHashedToken(final String hashedToken) {
        return this.emailVerificationTokenJpaRepository.findByTokenHash(hashedToken)
                .map(this.emailVerificationTokenInfraMapper::asRecord);
    }
}
