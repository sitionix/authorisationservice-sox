package com.sitionix.athssox.application.security;

import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.domain.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ExtendWith(MockitoExtension.class)
class LoginAuthenticationTokenTest {

    @Test
    void given_authenticated_token_when_set_authenticated_true_then_throw_exception() {
        //given
        final LoginAuthenticationToken given = LoginAuthenticationToken.authenticated(this.getAuthUser(1L, null));

        //when
        final Throwable actualThrowable = catchThrowable(() -> given.setAuthenticated(true));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Use authenticated factory method");
    }

    @Test
    void given_unauthenticated_token_when_set_authenticated_false_then_remain_unauthenticated() {
        //given
        final LoginAuthenticationToken given = LoginAuthenticationToken.unauthenticated("user@sitionix.com",
                "password",
                UUID.randomUUID());

        //when
        given.setAuthenticated(false);

        //then
        assertThat(given.isAuthenticated()).isFalse();
    }

    private AuthUser getAuthUser(final Long id, final UUID siteId) {
        return AuthUser.builder()
                .id(id)
                .email("user@sitionix.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .role(UserRole.SITE_USER)
                .siteId(siteId)
                .build();
    }
}
