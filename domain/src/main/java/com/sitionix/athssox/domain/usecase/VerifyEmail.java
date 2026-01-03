package com.sitionix.athssox.domain.usecase;

import com.sitionix.athssox.domain.model.emailverify.EmailVerification;

public interface VerifyEmail {

    boolean execute(EmailVerification emailVerification);
}
