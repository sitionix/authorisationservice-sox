package com.sitionix.athssox.api.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientIpResolverTest {

    private ClientIpResolver clientIpResolver;

    private SecurityProperties securityProperties;

    @Mock
    private HttpServletRequest httpServletRequest;

    @BeforeEach
    void setUp() {
        this.securityProperties = this.getSecurityProperties(false);
        this.clientIpResolver = new ClientIpResolver(this.securityProperties);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.httpServletRequest);
    }

    @Test
    void givenTrustedProxyAndForwardedForList_whenResolve_thenReturnFirstIp() {
        //given
        final String forwardedFor = this.getForwardedForHeader();
        final String expected = this.getForwardedForFirstIp();
        this.securityProperties.setTrustedProxy(true);

        when(this.httpServletRequest.getHeader("X-Forwarded-For"))
                .thenReturn(forwardedFor);

        //when
        final String actual = this.clientIpResolver.resolve(this.httpServletRequest);

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.httpServletRequest)
                .getHeader("X-Forwarded-For");
    }

    @Test
    void givenTrustedProxyDisabled_whenResolve_thenReturnRemoteAddr() {
        //given
        final String remoteAddr = this.getRemoteAddr();

        when(this.httpServletRequest.getRemoteAddr())
                .thenReturn(remoteAddr);

        //when
        final String actual = this.clientIpResolver.resolve(this.httpServletRequest);

        //then
        assertThat(actual).isEqualTo(remoteAddr);
        verify(this.httpServletRequest)
                .getRemoteAddr();
    }

    private SecurityProperties getSecurityProperties(final boolean trustedProxy) {
        final SecurityProperties properties = new SecurityProperties();
        properties.setTrustedProxy(trustedProxy);
        return properties;
    }

    private String getForwardedForHeader() {
        return "203.0.113.10, 70.41.3.18";
    }

    private String getForwardedForFirstIp() {
        return "203.0.113.10";
    }

    private String getRemoteAddr() {
        return "192.168.1.10";
    }
}
