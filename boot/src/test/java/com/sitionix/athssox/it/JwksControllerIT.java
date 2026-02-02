package com.sitionix.athssox.it;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.jayway.jsonpath.JsonPath;
import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class JwksControllerIT {

    @Autowired
    private TestManager testManager;

    @Test
    @DisplayName("Should allow JWKS access without authentication")
    void given_no_auth_when_request_jwks_then_ok() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.jwks())
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should return JWKS in valid format")
    void given_jwks_request_when_get_jwks_then_return_valid_key_format() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.jwks())
                .expectStatus(HttpStatus.OK)
                .expectResponse("jwksResponse.json")
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should match JWT kid with JWKS keys")
    void given_access_token_when_get_jwks_then_contains_token_kid() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

        final List<String> accessTokens = new ArrayList<>();
        final List<String> jwksBodies = new ArrayList<>();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .andExpectPath(result -> accessTokens.add(JsonPath.read(
                        result.getResponse().getContentAsString(),
                        "$.accessToken")))
                .assertAndCreate();

        this.testManager.mockMvc()
                .ping(ControllerEndpoint.jwks())
                .expectStatus(HttpStatus.OK)
                .andExpectPath(result -> jwksBodies.add(result.getResponse().getContentAsString()))
                .assertAndCreate();

        //then
        assertThat(accessTokens).hasSize(1);
        assertThat(jwksBodies).hasSize(1);

        final DecodedJWT decoded = JWT.decode(accessTokens.get(0));
        final String tokenKid = decoded.getKeyId();
        final List<String> jwksKids = JsonPath.read(jwksBodies.get(0), "$.keys[*].kid");

        assertThat(jwksKids).contains(tokenKid);
    }

    @Test
    @DisplayName("Should allow JWKS alias access without authentication")
    void givenNoAuth_whenRequestJwksAlias_thenOk() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.jwksAlias())
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should return JWKS alias in valid format")
    void givenJwksRequest_whenGetJwksAlias_thenReturnValidKeyFormat() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.jwksAlias())
                .expectStatus(HttpStatus.OK)
                .expectResponse("jwksResponse.json")
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should return identical JWKS payload for canonical and alias endpoints")
    void givenJwksRequest_whenCompareAliasAndCanonical_thenSameResponse() {
        //given
        final List<String> canonicalBodies = new ArrayList<>();
        final List<String> aliasBodies = new ArrayList<>();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.jwks())
                .expectStatus(HttpStatus.OK)
                .andExpectPath(result -> canonicalBodies.add(result.getResponse().getContentAsString()))
                .assertAndCreate();

        this.testManager.mockMvc()
                .ping(ControllerEndpoint.jwksAlias())
                .expectStatus(HttpStatus.OK)
                .andExpectPath(result -> aliasBodies.add(result.getResponse().getContentAsString()))
                .assertAndCreate();

        //then
        assertThat(canonicalBodies).hasSize(1);
        assertThat(aliasBodies).hasSize(1);
        assertThat(aliasBodies.get(0)).isEqualTo(canonicalBodies.get(0));
    }

    @Test
    @DisplayName("Should not expose private key fields in JWKS")
    void given_jwks_request_when_get_jwks_then_does_not_expose_private_key_fields() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.jwks())
                .expectStatus(HttpStatus.OK)
                .andExpectPath(MockMvcResultMatchers.jsonPath("$..d").doesNotExist())
                .andExpectPath(MockMvcResultMatchers.jsonPath("$..p").doesNotExist())
                .andExpectPath(MockMvcResultMatchers.jsonPath("$..q").doesNotExist())
                .andExpectPath(MockMvcResultMatchers.jsonPath("$..dp").doesNotExist())
                .andExpectPath(MockMvcResultMatchers.jsonPath("$..dq").doesNotExist())
                .andExpectPath(MockMvcResultMatchers.jsonPath("$..qi").doesNotExist())
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should not expose private key fields in JWKS alias")
    void givenJwksRequest_whenGetJwksAlias_thenDoesNotExposePrivateKeyFields() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.jwksAlias())
                .expectStatus(HttpStatus.OK)
                .andExpectPath(MockMvcResultMatchers.jsonPath("$..d").doesNotExist())
                .andExpectPath(MockMvcResultMatchers.jsonPath("$..p").doesNotExist())
                .andExpectPath(MockMvcResultMatchers.jsonPath("$..q").doesNotExist())
                .andExpectPath(MockMvcResultMatchers.jsonPath("$..dp").doesNotExist())
                .andExpectPath(MockMvcResultMatchers.jsonPath("$..dq").doesNotExist())
                .andExpectPath(MockMvcResultMatchers.jsonPath("$..qi").doesNotExist())
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should include cache headers for JWKS")
    void given_jwks_request_when_get_jwks_then_cache_headers_set() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.jwks())
                .expectStatus(HttpStatus.OK)
                .andExpectPath(MockMvcResultMatchers.header().string("Cache-Control", "max-age=5, public"))
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should include cache headers for JWKS alias")
    void givenJwksRequest_whenGetJwksAlias_thenCacheHeadersSet() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.jwksAlias())
                .expectStatus(HttpStatus.OK)
                .andExpectPath(MockMvcResultMatchers.header().string("Cache-Control", "max-age=5, public"))
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should return multiple keys for rotation and verify legacy token")
    void given_legacy_key_configured_when_get_jwks_then_contains_legacy_key_and_verifies_token() throws Exception {
        //given
        final String applicationItYaml;
        try (InputStream inputStream = Objects.requireNonNull(this.getClass().getClassLoader()
                .getResourceAsStream("application-it.yml"))) {
            applicationItYaml = new String(inputStream.readAllBytes(), StandardCharsets.US_ASCII);
        }

        final Pattern privatePattern = Pattern.compile(
                "-----BEGIN PRIVATE KEY-----.*?-----END PRIVATE KEY-----",
                Pattern.DOTALL);
        final Matcher privateMatcher = privatePattern.matcher(applicationItYaml);
        assertThat(privateMatcher.find()).isTrue();
        final String privateKeyPem = privateMatcher.group(0);

        final Pattern publicPattern = Pattern.compile(
                "-----BEGIN PUBLIC KEY-----.*?-----END PUBLIC KEY-----",
                Pattern.DOTALL);
        final Matcher publicMatcher = publicPattern.matcher(applicationItYaml);
        assertThat(publicMatcher.find()).isTrue();
        final String publicKeyPem = publicMatcher.group(0);

        final String normalizedPrivate = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        final byte[] privateKeyBytes = Base64.getDecoder().decode(normalizedPrivate);
        final PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        final RSAPrivateKey legacyPrivateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(privateKeySpec);

        final String normalizedPublic = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        final byte[] publicKeyBytes = Base64.getDecoder().decode(normalizedPublic);
        final X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        final RSAPublicKey legacyPublicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(publicKeySpec);
        final Algorithm legacyAlgorithm = Algorithm.RSA256(legacyPublicKey, legacyPrivateKey);
        final Instant now = Instant.now();
        final String legacyToken = JWT.create()
                .withIssuer("athssox")
                .withSubject("legacy-user")
                .withClaim("type", "access")
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(3600L)))
                .withKeyId("it-key-legacy")
                .sign(legacyAlgorithm);
        final List<String> jwksBodies = new ArrayList<>();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.jwks())
                .expectStatus(HttpStatus.OK)
                .andExpectPath(result -> jwksBodies.add(result.getResponse().getContentAsString()))
                .assertAndCreate();

        //then
        assertThat(jwksBodies).hasSize(1);
        final String jwksBody = jwksBodies.get(0);
        final List<String> kids = JsonPath.read(jwksBody, "$.keys[*].kid");
        assertThat(kids).hasSize(2);

        final List<String> modulus = JsonPath.read(jwksBody, "$.keys[?(@.kid=='it-key-legacy')].n");
        final List<String> exponent = JsonPath.read(jwksBody, "$.keys[?(@.kid=='it-key-legacy')].e");
        assertThat(modulus).hasSize(1);
        assertThat(exponent).hasSize(1);

        final BigInteger modulusValue = new BigInteger(1, Base64.getUrlDecoder().decode(modulus.get(0)));
        final BigInteger exponentValue = new BigInteger(1, Base64.getUrlDecoder().decode(exponent.get(0)));
        final RSAPublicKeySpec jwksKeySpec = new RSAPublicKeySpec(modulusValue, exponentValue);
        final RSAPublicKey jwksKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(jwksKeySpec);
        final Algorithm verifyAlgorithm = Algorithm.RSA256(jwksKey, null);
        final DecodedJWT verified = JWT.require(verifyAlgorithm)
                .withIssuer("athssox")
                .build()
                .verify(legacyToken);

        assertThat(verified.getKeyId()).isEqualTo("it-key-legacy");
    }
}
