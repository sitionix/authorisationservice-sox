package com.sitionix.athssox.api.ratelimit;

import com.sitionix.athssox.domain.service.RateLimitResult;
import com.sitionix.athssox.domain.service.RateLimiterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitGuardTest {

    private RateLimitGuard rateLimitGuard;

    private RateLimitProperties rateLimitProperties;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private EmailNormalizer emailNormalizer;

    @BeforeEach
    void setUp() {
        this.rateLimitProperties = this.getRateLimitProperties();
        this.rateLimitGuard = new RateLimitGuard(this.rateLimiterService,
                this.rateLimitProperties,
                this.emailNormalizer);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.rateLimiterService, this.emailNormalizer);
    }

    @Test
    void givenLoginIpAndSession_whenCheckLogin_thenConsumeCompositeKey() {
        //given
        final String ip = this.getIp();
        final String email = this.getEmail();
        final String normalizedEmail = this.getNormalizedEmail();
        final String sessionSourceId = this.getSessionSourceId();
        final Duration window = this.getWindow();

        this.configureRule(this.rateLimitProperties.getLogin().getIpSession(), 10L, window);
        this.rateLimitProperties.setEnabled(true);

        when(this.emailNormalizer.normalize(email))
                .thenReturn(normalizedEmail);
        when(this.rateLimiterService.consume(this.getLoginIpSessionKey(ip, sessionSourceId), 10L, window))
                .thenReturn(this.getAllowedResult());

        //when
        this.rateLimitGuard.checkLogin(ip, email, sessionSourceId);

        //then
        verify(this.emailNormalizer)
                .normalize(email);
        verify(this.rateLimiterService)
                .consume(this.getLoginIpSessionKey(ip, sessionSourceId), 10L, window);
    }

    @Test
    void givenRegisterIpAndEmail_whenCheckRegister_thenConsumeCompositeKey() {
        //given
        final String ip = this.getIp();
        final String email = this.getEmail();
        final String normalizedEmail = this.getNormalizedEmail();
        final Duration window = this.getWindow();

        this.configureRule(this.rateLimitProperties.getRegister().getIpEmail(), 3L, window);
        this.rateLimitProperties.setEnabled(true);

        when(this.emailNormalizer.normalize(email))
                .thenReturn(normalizedEmail);
        when(this.rateLimiterService.consume(this.getRegisterIpEmailKey(ip, normalizedEmail), 3L, window))
                .thenReturn(this.getAllowedResult());

        //when
        this.rateLimitGuard.checkRegister(ip, email);

        //then
        verify(this.emailNormalizer)
                .normalize(email);
        verify(this.rateLimiterService)
                .consume(this.getRegisterIpEmailKey(ip, normalizedEmail), 3L, window);
    }

    @Test
    void givenRefreshIpAndSession_whenCheckRefresh_thenConsumeCompositeKey() {
        //given
        final String ip = this.getIp();
        final String sessionSourceId = this.getSessionSourceId();
        final Duration window = this.getWindow();

        this.configureRule(this.rateLimitProperties.getRefresh().getIpSession(), 30L, window);
        this.rateLimitProperties.setEnabled(true);

        when(this.rateLimiterService.consume(this.getRefreshIpSessionKey(ip, sessionSourceId), 30L, window))
                .thenReturn(this.getAllowedResult());

        //when
        this.rateLimitGuard.checkRefresh(ip, sessionSourceId);

        //then
        verify(this.rateLimiterService)
                .consume(this.getRefreshIpSessionKey(ip, sessionSourceId), 30L, window);
    }

    @Test
    void givenNonIpValue_whenMaskIp_thenReturnOriginal() {
        //given
        final String ip = this.getNonIpValue();

        //when
        final String actual = this.callMaskIp(ip);

        //then
        assertThat(actual).isEqualTo(ip);
    }

    private RateLimitProperties getRateLimitProperties() {
        return new RateLimitProperties();
    }

    private void configureRule(final RateLimitProperties.Rule rule, final long limit, final Duration window) {
        rule.setEnabled(true);
        rule.setLimit(limit);
        rule.setWindow(window);
    }

    private RateLimitResult getAllowedResult() {
        return RateLimitResult.allow();
    }

    private Duration getWindow() {
        return Duration.ofMinutes(5);
    }

    private String getIp() {
        return "127.0.0.1";
    }

    private String getEmail() {
        return "USER@SITIONIX.COM";
    }

    private String getNormalizedEmail() {
        return "user@sitionix.com";
    }

    private String getSessionSourceId() {
        return "device-123";
    }

    private String getNonIpValue() {
        return "not-an-ip";
    }

    private String getLoginIpSessionKey(final String ip, final String sessionSourceId) {
        return "login:ip-session:" + ip + ":" + sessionSourceId;
    }

    private String getRegisterIpEmailKey(final String ip, final String email) {
        return "register:ip-email:" + ip + ":" + email;
    }

    private String getRefreshIpSessionKey(final String ip, final String sessionSourceId) {
        return "refresh:ip-session:" + ip + ":" + sessionSourceId;
    }

    private String callMaskIp(final String ip) {
        try {
            final Method method = RateLimitGuard.class.getDeclaredMethod("maskIp", String.class);
            method.setAccessible(true);
            return (String) method.invoke(this.rateLimitGuard, ip);
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to invoke maskIp.", ex);
        }
    }
}
