package com.sitionix.athssox.application.service;

import com.sitionix.athssox.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenRevocationService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean revokeIfActive(final Long tokenId, final Instant now, final String reason) {
        if (tokenId == null) {
            return false;
        }
        return this.refreshTokenRepository.revokeIfActive(tokenId, now, reason);
    }
}
