package com.sitionix.athssox.application.service;

import com.sitionix.athssox.domain.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

@Service
public class RefreshTokenRevocationService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TransactionTemplate transactionTemplate;

    public RefreshTokenRevocationService(final RefreshTokenRepository refreshTokenRepository,
                                         final PlatformTransactionManager transactionManager) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public boolean revokeIfActive(final Long tokenId, final Instant now, final String reason) {
        if (tokenId == null) {
            return false;
        }
        final Boolean revoked = this.transactionTemplate.execute(
                status -> this.refreshTokenRepository.revokeIfActive(tokenId, now, reason));
        return Boolean.TRUE.equals(revoked);
    }
}
