package com.sitionix.athssox.application.usecase;

import com.sitionix.athssox.application.validator.PasswordPolicyValidator;
import com.sitionix.athssox.domain.builder.EmailVerifyPayloadBuilder;
import com.sitionix.athssox.domain.model.RegisterUserDO;
import com.sitionix.athssox.domain.model.ResponseRegisterUser;
import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.domain.model.UserStatus;
import com.sitionix.athssox.domain.exception.EmailAlreadyRegisteredException;
import com.sitionix.athssox.domain.exception.MissingSiteIdException;
import com.sitionix.athssox.domain.model.emailverify.EmailVerifyPayloadContext;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.repository.UserRepository;
import com.sitionix.athssox.domain.service.EmailVerificationResendPolicy;
import com.sitionix.athssox.domain.usecase.RegisterUser;
import com.sitionix.forge.outbox.core.command.ForgeOutboxCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUserImpl implements RegisterUser {

    private static final String REGISTRATION_ACCEPTED_MESSAGE =
            "Registration accepted. Please check your email for verification.";
    private static final String REGISTRATION_ALREADY_PROCESSED_MESSAGE =
            "Registration already processed. Please check your email.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final ForgeOutboxCommand<EmailVerifyPayload> command;
    private final EmailVerifyPayloadBuilder emailVerifyPayloadBuilder;
    private final EmailVerificationResendPolicy emailVerificationResendPolicy;
    private final Clock clock;

    @Override
    @Transactional
    public ResponseRegisterUser execute(@Valid final RegisterUserDO registerUserDO) {

        this.validateSiteScope(registerUserDO);
        final Optional<ResponseRegisterUser> existingUser = this.findExistingUser(registerUserDO);
        if (existingUser.isPresent() && existingUser.get().getStatus() == UserStatus.PENDING_EMAIL_VERIFY) {
            return this.handlePendingUser(existingUser.get(), registerUserDO);
        }

        this.passwordPolicyValidator.validate(registerUserDO.getPassword());
        if (existingUser.isPresent()) {
            throw new EmailAlreadyRegisteredException(REGISTRATION_ALREADY_PROCESSED_MESSAGE);
        }
        registerUserDO.setPassword(this.passwordEncoder.encode(registerUserDO.getPassword()));

        final ResponseRegisterUser createdUser = this.userRepository.createUser(registerUserDO);

        final EmailVerifyPayload payload = this.emailVerifyPayloadBuilder.build(this.buildContext(createdUser, registerUserDO));
        this.command.send(payload);

        createdUser.setMessage(REGISTRATION_ACCEPTED_MESSAGE);
        return createdUser;
    }

    private void validateSiteScope(final RegisterUserDO registerUserDO) {
        final UserRole role = registerUserDO.getRole();
        if (role == null) {
            return;
        }
        if (role.isSiteScoped() && registerUserDO.getSiteId() == null) {
            throw new MissingSiteIdException("siteId is required for site-scoped roles");
        }
        if (role.isGlobalScoped()) {
            registerUserDO.setSiteId(null);
        }
    }

    private EmailVerifyPayloadContext buildContext(final ResponseRegisterUser createdUser, final RegisterUserDO registerUserDO) {
        return new EmailVerifyPayloadContext(
                createdUser.getUserId(),
                registerUserDO.getSiteId(),
                registerUserDO.getEmail(),
                null,
                null,
                this.clock.instant()
        );
    }

    private Optional<ResponseRegisterUser> findExistingUser(final RegisterUserDO registerUserDO) {
        final UserRole role = registerUserDO.getRole();
        final String email = registerUserDO.getEmail();
        final UUID siteId = registerUserDO.getSiteId();

        if (role == null) {
            return Optional.empty();
        }

        if (role.isSiteScoped()) {
            return this.userRepository.findSiteScopedByEmailAndSiteId(email, siteId);
        }

        if (role.isGlobalScoped()) {
            return this.userRepository.findGlobalByEmail(email);
        }

        return Optional.empty();
    }

    private ResponseRegisterUser handlePendingUser(final ResponseRegisterUser existingUser,
                                                   final RegisterUserDO registerUserDO) {
        if (this.emailVerificationResendPolicy.isResendAllowed(existingUser.getUserId())) {
            final EmailVerifyPayload payload = this.emailVerifyPayloadBuilder
                    .build(this.buildContext(existingUser, registerUserDO));
            this.command.send(payload);
        }

        existingUser.setMessage(REGISTRATION_ACCEPTED_MESSAGE);
        return existingUser;
    }
}
