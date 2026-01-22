package com.sitionix.athssox.domain.usecase;

import com.sitionix.athssox.domain.model.emailverify.EmailVerificationLinkIssue;

import java.util.UUID;

public interface IssueEmailVerificationLink {

    EmailVerificationLinkIssue execute(UUID tokenId, UUID pepperId);
}
