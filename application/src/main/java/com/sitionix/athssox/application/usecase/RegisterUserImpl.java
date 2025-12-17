package com.sitionix.athssox.application.usecase;

import com.sitionix.athssox.application.validator.PasswordPolicyValidator;
import com.sitionix.athssox.domain.model.RegisterUserDO;
import com.sitionix.athssox.domain.model.ResponseRegisterUser;
import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.domain.exception.EmailAlreadyRegisteredException;
import com.sitionix.athssox.domain.repository.UserRepository;
import com.sitionix.athssox.domain.usecase.RegisterUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterUserImpl implements RegisterUser {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;

    @Override
    public ResponseRegisterUser execute(@Valid final RegisterUserDO registerUserDO) {

        this.passwordPolicyValidator.validate(registerUserDO.getPassword());
        this.validateEmailUniqueness(registerUserDO);
        registerUserDO.setPassword(this.passwordEncoder.encode(registerUserDO.getPassword()));

        final ResponseRegisterUser createdUser = this.userRepository.createUser(registerUserDO);
        createdUser.setMessage("Registration successful. Please verify your email.");
        return createdUser;
    }

    private void validateEmailUniqueness(final RegisterUserDO registerUserDO) {
        final UserRole role = registerUserDO.getRole();
        final String email = registerUserDO.getEmail();
        final UUID siteId = registerUserDO.getSiteId();

        if (role == UserRole.SITE_USER || role == UserRole.SITE_ADMIN) {
            if (this.userRepository.existsSiteScopedByEmailAndSiteId(email, siteId)) {
                throw new EmailAlreadyRegisteredException("Email already registered for this site.");
            }
            return;
        }

        if (role == UserRole.SUPER_ADMIN || role == UserRole.ECOSYSTEM_OWNER) {
            if (this.userRepository.existsGlobalByEmail(email)) {
                throw new EmailAlreadyRegisteredException("Email already registered for this role scope.");
            }
        }
    }
}
