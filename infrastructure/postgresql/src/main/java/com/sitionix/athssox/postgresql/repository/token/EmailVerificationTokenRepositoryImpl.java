package com.sitionix.athssox.postgresql.repository.token;

import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenRecord;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.postgresql.entity.token.EmailVerificationTokenEntity;
import com.sitionix.athssox.postgresql.jpa.token.EmailVerificationTokenJpaRepository;
import com.sitionix.athssox.postgresql.mapper.token.EmailVerificationTokenInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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
    public Optional<EmailVerificationTokenRecord> findByHashedToken(final String hashedToken) {
        return this.emailVerificationTokenJpaRepository.findByTokenHash(hashedToken)
                .map(this.emailVerificationTokenInfraMapper::asRecord);
    }

}
