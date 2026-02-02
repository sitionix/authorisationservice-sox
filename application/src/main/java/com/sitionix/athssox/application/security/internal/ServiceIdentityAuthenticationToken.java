package com.sitionix.athssox.application.security.internal;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ServiceIdentityAuthenticationToken extends AbstractAuthenticationToken {

    private final ServiceIdentity identity;

    private ServiceIdentityAuthenticationToken(final ServiceIdentity identity,
                                               final Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.identity = identity;
    }

    public static ServiceIdentityAuthenticationToken authenticated(final ServiceIdentity identity) {
        final List<GrantedAuthority> authorities = identity.scopes() == null
                ? List.of()
                : identity.scopes().stream()
                .filter(Objects::nonNull)
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .toList();
        final ServiceIdentityAuthenticationToken token = new ServiceIdentityAuthenticationToken(identity, authorities);
        token.setAuthenticated(true);
        return token;
    }

    public ServiceIdentity getIdentity() {
        return this.identity;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return this.identity;
    }
}
