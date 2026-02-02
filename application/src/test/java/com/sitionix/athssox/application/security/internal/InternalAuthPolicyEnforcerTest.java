package com.sitionix.athssox.application.security.internal;

import com.sitionix.athssox.application.config.InternalAuthConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ExtendWith(MockitoExtension.class)
class InternalAuthPolicyEnforcerTest {

    private InternalAuthPolicyEnforcer internalAuthPolicyEnforcer;

    @BeforeEach
    void setUp() {
        final InternalAuthConfig config = this.getInternalAuthConfig();
        this.internalAuthPolicyEnforcer = new InternalAuthPolicyEnforcer(config);
    }

    @Test
    void given_matching_scope_when_authorize_then_allow() {
        //given
        final ServiceIdentity identity = this.getServiceIdentity("notificationservice-sox",
                List.of("email.verify.link.issue"));
        final String requestPath = "/api/v1/auth/emailVerificationTokens/123:issueLink";

        //when
        final Throwable actualThrowable = catchThrowable(() ->
                this.internalAuthPolicyEnforcer.assertAllowed(identity, requestPath));

        //then
        assertThat(actualThrowable).isNull();
    }

    @Test
    void given_matching_endpoint_when_authorize_then_allow() {
        //given
        final InternalAuthConfig config = this.getInternalAuthConfigWithEndpointPolicy();
        this.internalAuthPolicyEnforcer = new InternalAuthPolicyEnforcer(config);
        final ServiceIdentity identity = this.getServiceIdentity("notificationservice-sox", List.of());
        final String requestPath = "/api/v1/auth/emailVerificationTokens/123:issueLink";

        //when
        final Throwable actualThrowable = catchThrowable(() ->
                this.internalAuthPolicyEnforcer.assertAllowed(identity, requestPath));

        //then
        assertThat(actualThrowable).isNull();
    }

    @Test
    void given_missing_policy_when_authorize_then_throw_access_denied_exception() {
        //given
        final ServiceIdentity identity = this.getServiceIdentity("unknownservice-sox", List.of("email.verify.link.issue"));
        final String requestPath = "/api/v1/auth/emailVerificationTokens/123:issueLink";

        //when
        final Throwable actualThrowable = catchThrowable(() ->
                this.internalAuthPolicyEnforcer.assertAllowed(identity, requestPath));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Internal service not allowed");
    }

    @Test
    void given_missing_scope_when_authorize_then_throw_access_denied_exception() {
        //given
        final ServiceIdentity identity = this.getServiceIdentity("notificationservice-sox", List.of("other.scope"));
        final String requestPath = "/api/v1/auth/emailVerificationTokens/123:issueLink";

        //when
        final Throwable actualThrowable = catchThrowable(() ->
                this.internalAuthPolicyEnforcer.assertAllowed(identity, requestPath));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Internal service not allowed");
    }

    private InternalAuthConfig getInternalAuthConfig() {
        final InternalAuthConfig config = new InternalAuthConfig();
        final InternalAuthConfig.PolicyConfig policy = new InternalAuthConfig.PolicyConfig();
        policy.setAllow(List.of("email.verify.link.issue"));
        config.setPolicies(Map.of("notificationservice-sox", policy));
        return config;
    }

    private InternalAuthConfig getInternalAuthConfigWithEndpointPolicy() {
        final InternalAuthConfig config = new InternalAuthConfig();
        final InternalAuthConfig.PolicyConfig policy = new InternalAuthConfig.PolicyConfig();
        policy.setAllow(List.of("/api/v1/auth/emailVerificationTokens/**:issueLink"));
        config.setPolicies(Map.of("notificationservice-sox", policy));
        return config;
    }

    private ServiceIdentity getServiceIdentity(final String serviceName, final List<String> scopes) {
        return new ServiceIdentity(serviceName, "sitionix-internal", "athssox", scopes);
    }
}
