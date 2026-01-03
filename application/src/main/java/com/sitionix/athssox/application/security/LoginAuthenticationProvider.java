package com.sitionix.athssox.application.security;

import com.sitionix.athssox.domain.exception.InactiveUserException;
import com.sitionix.athssox.domain.exception.MissingSiteIdException;
import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.model.UserStatus;
import com.sitionix.athssox.domain.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LoginAuthenticationProvider implements AuthenticationProvider {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(final Authentication authentication) {
        final LoginAuthenticationToken token = (LoginAuthenticationToken) authentication;
        final AuthUser user = this.loadUser(token.getEmail(), token.getSiteId());

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InactiveUserException("Account is not yet activated");
        }

        if (!this.passwordEncoder.matches((String) token.getCredentials(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return LoginAuthenticationToken.authenticated(user);
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return LoginAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private AuthUser loadUser(final String email, final UUID siteId) {
        Optional<AuthUser> user;
        if (siteId != null) {
            user = this.authUserRepository.findByEmailAndSiteId(email, siteId);
            if (user.isEmpty()) {
                user = this.authUserRepository.findGlobalByEmail(email);
            }
        } else {
            user = this.authUserRepository.findGlobalByEmail(email);
            if (user.isEmpty() && this.authUserRepository.existsSiteScopedByEmail(email)) {
                throw new MissingSiteIdException("siteId is required for site-scoped roles");
            }
        }

        return user.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
    }
}
