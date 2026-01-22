package com.sitionix.athssox.application.service;

import com.sitionix.athssox.application.config.EmailVerificationSecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class HmacEmailVerificationTokenSignerTest {

    private EmailVerificationSecurityConfig securityConfig;
    private HmacEmailVerificationTokenSigner signer;

    @BeforeEach
    void setUp() {
        this.securityConfig = this.getSecurityConfig(this.getSecret());
        this.signer = new HmacEmailVerificationTokenSigner(this.securityConfig);
    }

    @Test
    void givenTokenIdAndPepperId_whenSign_thenReturnUrlSafeSignature() {
        //given
        final UUID tokenId = this.getTokenId();
        final UUID pepperId = this.getPepperId();

        //when
        final String actual = this.signer.sign(tokenId, pepperId);

        //then
        assertThat(actual).isNotBlank();
        assertThat(actual).matches("^[A-Za-z0-9_-]+$");
        assertThat(actual).doesNotContain("=");
    }

    @Test
    void givenSameInputs_whenSign_thenSignatureIsStable() {
        //given
        final UUID tokenId = this.getTokenId();
        final UUID pepperId = this.getPepperId();

        //when
        final String first = this.signer.sign(tokenId, pepperId);
        final String second = this.signer.sign(tokenId, pepperId);

        //then
        assertThat(first).isEqualTo(second);
    }

    @Test
    void givenDifferentPepperIds_whenSign_thenSignaturesDiffer() {
        //given
        final UUID tokenId = this.getTokenId();
        final UUID firstPepper = this.getPepperId();
        final UUID secondPepper = this.getOtherPepperId();

        //when
        final String first = this.signer.sign(tokenId, firstPepper);
        final String second = this.signer.sign(tokenId, secondPepper);

        //then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void givenTokenIdAndPepperId_whenBuildToken_thenReturnTokenWithSignature() {
        //given
        final UUID tokenId = this.getTokenId();
        final UUID pepperId = this.getPepperId();

        //when
        final String actual = this.signer.buildToken(tokenId, pepperId);

        //then
        final String expected = tokenId + "." + pepperId + "." + this.signer.sign(tokenId, pepperId);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenBlankSecret_whenValidate_thenThrowException() {
        //given
        final EmailVerificationSecurityConfig config = this.getSecurityConfig(" ");
        final HmacEmailVerificationTokenSigner given = new HmacEmailVerificationTokenSigner(config);

        //when
        final Throwable actual = catchThrowable(given::validateSecret);

        //then
        assertThat(actual).isInstanceOf(IllegalStateException.class)
                .hasMessage("Email verification HMAC secret must be configured.");
    }

    private Throwable catchThrowable(final Runnable action) {
        try {
            action.run();
            return null;
        } catch (final Throwable throwable) {
            return throwable;
        }
    }

    private UUID getTokenId() {
        return UUID.fromString("8f24d9f6-2c05-4b77-8c4e-1bc6e1ba9b6c");
    }

    private UUID getPepperId() {
        return UUID.fromString("d5d2d5de-6930-43c0-9e45-9a8e6dbe8292");
    }

    private UUID getOtherPepperId() {
        return UUID.fromString("9a5e4094-9e30-41af-9f2b-0b7fbc07b4e5");
    }

    private String getSecret() {
        return "secret-value";
    }

    private EmailVerificationSecurityConfig getSecurityConfig(final String secret) {
        final EmailVerificationSecurityConfig config = new EmailVerificationSecurityConfig();
        config.setHmacSecret(secret);
        return config;
    }
}
