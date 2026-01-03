package com.sitionix.athssox.application.usecase;

import com.sitionix.athssox.application.validator.PasswordPolicyValidator;
import com.sitionix.athssox.domain.builder.OutboxEventBuilder;
import com.sitionix.athssox.domain.command.OutboxCommand;
import com.sitionix.athssox.domain.model.RegisterUserDO;
import com.sitionix.athssox.domain.model.ResponseRegisterUser;
import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.domain.exception.EmailAlreadyRegisteredException;
import com.sitionix.athssox.domain.exception.MissingSiteIdException;
import com.sitionix.athssox.domain.model.outbox.OutboxBuildContext;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.repository.UserRepository;
import com.sitionix.athssox.domain.usecase.RegisterUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterUserImpl implements RegisterUser {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final OutboxCommand<EmailVerifyPayload> command;
    private final OutboxEventBuilder<EmailVerifyPayload> outboxEventBuilder;

    @Override
    @Transactional
    public ResponseRegisterUser execute(@Valid final RegisterUserDO registerUserDO) {

        this.validateSiteScope(registerUserDO);
        this.passwordPolicyValidator.validate(registerUserDO.getPassword());
        this.validateEmailUniqueness(registerUserDO);
        registerUserDO.setPassword(this.passwordEncoder.encode(registerUserDO.getPassword()));

        final ResponseRegisterUser createdUser = this.userRepository.createUser(registerUserDO);

        final OutboxEvent<EmailVerifyPayload> outboxEvent = this.outboxEventBuilder.build(this.buildContext(createdUser, registerUserDO));

        this.command.execute(outboxEvent);

        createdUser.setMessage("Registration successful. Please verify your email.");
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

    private OutboxBuildContext buildContext(final ResponseRegisterUser createdUser, final RegisterUserDO registerUserDO) {
        return new OutboxBuildContext(
                createdUser.getUserId(),
                registerUserDO.getSiteId(),
                registerUserDO.getEmail(),
                null,
                null,
                Instant.now()
        );
    }

    private void validateEmailUniqueness(final RegisterUserDO registerUserDO) {
        final UserRole role = registerUserDO.getRole();
        final String email = registerUserDO.getEmail();
        final UUID siteId = registerUserDO.getSiteId();

        if (role == null) {
            return;
        }

        if (role.isSiteScoped()) {
            if (this.userRepository.existsSiteScopedByEmailAndSiteId(email, siteId)) {
                throw new EmailAlreadyRegisteredException("Email already registered for this site.");
            }
            return;
        }

        if (role.isGlobalScoped()) {
            if (this.userRepository.existsGlobalByEmail(email)) {
                throw new EmailAlreadyRegisteredException("Email already registered for this role scope.");
            }
        }
    }
}
