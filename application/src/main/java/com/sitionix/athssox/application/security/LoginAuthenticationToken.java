package com.sitionix.athssox.application.security;

import com.sitionix.athssox.domain.model.AuthUser;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LoginAuthenticationToken extends AbstractAuthenticationToken {

    private final String email;
    private final String password;
    private final UUID siteId;
    private final AuthUser user;

    private LoginAuthenticationToken(final String email,
                                     final String password,
                                     final UUID siteId,
                                     final AuthUser user,
                                     final Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.email = email;
        this.password = password;
        this.siteId = siteId;
        this.user = user;
        super.setAuthenticated(user != null);
    }

    public static LoginAuthenticationToken unauthenticated(final String email,
                                                           final String password,
                                                           final UUID siteId) {
        return new LoginAuthenticationToken(email,
                password,
                siteId,
                null,
                Collections.emptyList());
    }

    public static LoginAuthenticationToken authenticated(final AuthUser user) {
        return new LoginAuthenticationToken(user.getEmail(),
                null,
                user.getSiteId(),
                user,
                Collections.emptyList());
    }

    @Override
    public Object getCredentials() {
        return this.password;
    }

    @Override
    public Object getPrincipal() {
        return this.user;
    }

    @Override
    public void setAuthenticated(final boolean isAuthenticated) {
        if (isAuthenticated) {
            throw new IllegalArgumentException("Use authenticated factory method");
        }
        super.setAuthenticated(false);
    }

}
