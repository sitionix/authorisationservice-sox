package com.sitionix.athssox.api.ratelimit;

import com.sitionix.athssox.domain.service.RateLimitResult;
import com.sitionix.athssox.domain.service.RateLimiterService;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    void givenRateLimitDisabled_whenCheckLogin_thenSkipProcessing() {
        //given
        final String ip = this.getIp();
        final String email = this.getEmail();
        final String sessionSourceId = this.getSessionSourceId();
        this.rateLimitProperties.setEnabled(false);

        //when
        this.rateLimitGuard.checkLogin(ip, email, sessionSourceId);

        //then
        verifyNoInteractions(this.rateLimiterService, this.emailNormalizer);
    }

    @Test
    void givenBlankNormalizedEmail_whenResetLoginEmail_thenSkipReset() {
        //given
        final String email = this.getEmail();
        final String normalizedEmail = this.getBlankValue();

        when(this.emailNormalizer.normalize(email))
                .thenReturn(normalizedEmail);

        //when
        this.rateLimitGuard.resetLoginEmail(email);

        //then
        verify(this.emailNormalizer)
                .normalize(email);
        verifyNoInteractions(this.rateLimiterService);
    }

    @Test
    void givenBlankIpWithActiveRule_whenCheckLogin_thenSkipConsumption() {
        //given
        final String ip = this.getBlankValue();
        final String email = this.getEmail();
        final String normalizedEmail = this.getNormalizedEmail();
        final String sessionSourceId = this.getSessionSourceId();
        final Duration window = this.getWindow();

        this.configureRule(this.rateLimitProperties.getLogin().getIp(), 1L, window);
        this.rateLimitProperties.setEnabled(true);

        when(this.emailNormalizer.normalize(email))
                .thenReturn(normalizedEmail);

        //when
        this.rateLimitGuard.checkLogin(ip, email, sessionSourceId);

        //then
        verify(this.emailNormalizer)
                .normalize(email);
        verifyNoInteractions(this.rateLimiterService);
    }

    @Test
    void givenLimitedIpRule_whenCheckLogin_thenThrowRateLimitExceeded() {
        //given
        final String ip = this.getLoginIp();
        final String email = this.getEmail();
        final String normalizedEmail = this.getNormalizedEmail();
        final String sessionSourceId = this.getSessionSourceId();
        final Duration window = this.getDurationSeconds(5L);
        final long retryAfterSeconds = 5L;
        final String expectedMessage = this.getRetryAfterMessage(retryAfterSeconds);
        final String traceId = this.getTraceId();

        this.configureRule(this.rateLimitProperties.getLogin().getIp(), 1L, window);
        this.rateLimitProperties.setEnabled(true);

        when(this.emailNormalizer.normalize(email))
                .thenReturn(normalizedEmail);
        when(this.rateLimiterService.consume(this.getLoginIpKey(ip), 1L, window))
                .thenReturn(this.getLimitedResult(window));

        ThreadContext.put(this.getTraceIdKey(), traceId);
        try {
            //when
            assertThatThrownBy(() -> this.rateLimitGuard.checkLogin(ip, email, sessionSourceId))
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessage(expectedMessage)
                    .satisfies(ex -> assertThat(((RateLimitExceededException) ex).getRetryAfterSeconds())
                            .isEqualTo(retryAfterSeconds));
        } finally {
            ThreadContext.remove(this.getTraceIdKey());
        }

        //then
        verify(this.emailNormalizer)
                .normalize(email);
        verify(this.rateLimiterService)
                .consume(this.getLoginIpKey(ip), 1L, window);
    }

    @Test
    void givenActiveRefreshRules_whenCheckRefresh_thenConsumeAllRules() {
        //given
        final String ip = this.getIpv6();
        final String sessionSourceId = this.getSessionSourceId();
        final Duration window = this.getWindow();

        this.configureRule(this.rateLimitProperties.getRefresh().getIp(), 60L, window);
        this.configureRule(this.rateLimitProperties.getRefresh().getSession(), 30L, window);
        this.configureRule(this.rateLimitProperties.getRefresh().getIpSession(), 30L, window);
        this.rateLimitProperties.setEnabled(true);

        when(this.rateLimiterService.consume(this.getRefreshIpKey(ip), 60L, window))
                .thenReturn(this.getAllowedResult());
        when(this.rateLimiterService.consume(this.getRefreshSessionKey(sessionSourceId), 30L, window))
                .thenReturn(this.getAllowedResult());
        when(this.rateLimiterService.consume(this.getRefreshIpSessionKey(ip, sessionSourceId), 30L, window))
                .thenReturn(this.getAllowedResult());

        //when
        this.rateLimitGuard.checkRefresh(ip, sessionSourceId);

        //then
        verify(this.rateLimiterService)
                .consume(this.getRefreshIpKey(ip), 60L, window);
        verify(this.rateLimiterService)
                .consume(this.getRefreshSessionKey(sessionSourceId), 30L, window);
        verify(this.rateLimiterService)
                .consume(this.getRefreshIpSessionKey(ip, sessionSourceId), 30L, window);
    }

    @Test
    void givenActiveResendRules_whenCheckResend_thenConsumeAllRules() {
        //given
        final String ip = this.getIp();
        final String email = this.getEmail();
        final String normalizedEmail = this.getNormalizedEmail();
        final Duration window = this.getWindow();

        this.configureRule(this.rateLimitProperties.getResend().getIp(), 5L, window);
        this.configureRule(this.rateLimitProperties.getResend().getEmail(), 3L, window);
        this.configureRule(this.rateLimitProperties.getResend().getIpEmail(), 3L, window);
        this.rateLimitProperties.setEnabled(true);

        when(this.emailNormalizer.normalize(email))
                .thenReturn(normalizedEmail);
        when(this.rateLimiterService.consume(this.getResendIpKey(ip), 5L, window))
                .thenReturn(this.getAllowedResult());
        when(this.rateLimiterService.consume(this.getResendEmailKey(normalizedEmail), 3L, window))
                .thenReturn(this.getAllowedResult());
        when(this.rateLimiterService.consume(this.getResendIpEmailKey(ip, normalizedEmail), 3L, window))
                .thenReturn(this.getAllowedResult());

        //when
        this.rateLimitGuard.checkResend(ip, email);

        //then
        verify(this.emailNormalizer)
                .normalize(email);
        verify(this.rateLimiterService)
                .consume(this.getResendIpKey(ip), 5L, window);
        verify(this.rateLimiterService)
                .consume(this.getResendEmailKey(normalizedEmail), 3L, window);
        verify(this.rateLimiterService)
                .consume(this.getResendIpEmailKey(ip, normalizedEmail), 3L, window);
    }

    @Test
    void givenBlankNormalizedEmail_whenCheckRegister_thenConsumeOnlyIpRule() {
        //given
        final String ip = this.getIp();
        final String email = this.getEmail();
        final String normalizedEmail = this.getBlankValue();
        final Duration window = this.getWindow();

        this.configureRule(this.rateLimitProperties.getRegister().getIp(), 5L, window);
        this.rateLimitProperties.setEnabled(true);

        when(this.emailNormalizer.normalize(email))
                .thenReturn(normalizedEmail);
        when(this.rateLimiterService.consume(this.getRegisterIpKey(ip), 5L, window))
                .thenReturn(this.getAllowedResult());

        //when
        this.rateLimitGuard.checkRegister(ip, email);

        //then
        verify(this.emailNormalizer)
                .normalize(email);
        verify(this.rateLimiterService)
                .consume(this.getRegisterIpKey(ip), 5L, window);
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

    private RateLimitResult getLimitedResult(final Duration retryAfter) {
        return RateLimitResult.limited(retryAfter);
    }

    private Duration getWindow() {
        return Duration.ofMinutes(5);
    }

    private Duration getDurationSeconds(final long seconds) {
        return Duration.ofSeconds(seconds);
    }

    private String getIp() {
        return "127.0.0.1";
    }

    private String getLoginIp() {
        return "192.168.1.55";
    }

    private String getIpv6() {
        return "2001:db8:85a3:0:0:8a2e:370:7334";
    }

    private String getEmail() {
        return "USER@SITIONIX.COM";
    }

    private String getNormalizedEmail() {
        return "user@sitionix.com";
    }

    private String getBlankValue() {
        return " ";
    }

    private String getSessionSourceId() {
        return "device-123";
    }

    private String getNonIpValue() {
        return "not-an-ip";
    }

    private String getTraceId() {
        return "trace-123";
    }

    private String getTraceIdKey() {
        return "traceId";
    }

    private String getRetryAfterMessage(final long retryAfterSeconds) {
        return "Too many requests. Please retry after " + retryAfterSeconds + " seconds.";
    }

    private String getLoginIpKey(final String ip) {
        return "login:ip:" + ip;
    }

    private String getLoginIpSessionKey(final String ip, final String sessionSourceId) {
        return "login:ip-session:" + ip + ":" + sessionSourceId;
    }

    private String getRegisterIpKey(final String ip) {
        return "register:ip:" + ip;
    }

    private String getRegisterIpEmailKey(final String ip, final String email) {
        return "register:ip-email:" + ip + ":" + email;
    }

    private String getResendIpKey(final String ip) {
        return "resend:ip:" + ip;
    }

    private String getResendEmailKey(final String email) {
        return "resend:email:" + email;
    }

    private String getResendIpEmailKey(final String ip, final String email) {
        return "resend:ip-email:" + ip + ":" + email;
    }

    private String getRefreshIpKey(final String ip) {
        return "refresh:ip:" + ip;
    }

    private String getRefreshSessionKey(final String sessionSourceId) {
        return "refresh:session:" + sessionSourceId;
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
