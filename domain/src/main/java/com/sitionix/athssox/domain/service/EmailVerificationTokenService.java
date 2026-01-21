package com.sitionix.athssox.domain.service;

import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenIssue;

import java.util.UUID;

public interface EmailVerificationTokenService {
    EmailVerificationTokenIssue issue(Long userId, UUID siteId);
}
