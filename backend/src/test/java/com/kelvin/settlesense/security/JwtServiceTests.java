package com.kelvin.settlesense.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;

class JwtServiceTests {

    private JwtService jwtService;

    // Base64 encoded version of "test-secret-key-for-jwt-signing-minimum-256-bits-required"
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3Qtc2lnbmluZy1taW5pbXVtLTI1Ni1iaXRzLXJlcXVpcmVk";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "key", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L);
    }

    @Test
    void generateTokenCreatesValidToken() {
        String token = jwtService.generateToken(new HashMap<>(), "testuser");

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    void generateTokenWithClaimsIncludesClaims() {
        HashMap<String, String> claims = new HashMap<>();
        claims.put("email", "test@example.com");
        claims.put("displayName", "Test User");

        String token = jwtService.generateToken(claims, "testuser");

        String email = jwtService.extractClaim(token, c -> c.get("email", String.class));
        String displayName = jwtService.extractClaim(token, c -> c.get("displayName", String.class));
        assertThat(email).isEqualTo("test@example.com");
        assertThat(displayName).isEqualTo("Test User");
    }

    @Test
    void extractDisplayNameReturnsClaimValue() {
        HashMap<String, String> claims = new HashMap<>();
        claims.put("displayName", "Test User");

        String token = jwtService.generateToken(claims, "testuser");

        assertThat(jwtService.extractDisplayName(token)).isEqualTo("Test User");
    }

    @Test
    void extractEmailReturnsClaimValue() {
        HashMap<String, String> claims = new HashMap<>();
        claims.put("email", "test@example.com");

        String token = jwtService.generateToken(claims, "testuser");

        assertThat(jwtService.extractEmail(token)).isEqualTo("test@example.com");
    }

    @Test
    void isTokenValidReturnsTrueForValidToken() {
        String token = jwtService.generateToken(new HashMap<>(), "testuser");

        boolean valid = jwtService.isTokenValid(token, "testuser");

        assertThat(valid).isTrue();
    }

    @Test
    void isTokenValidReturnsFalseForWrongSubject() {
        String token = jwtService.generateToken(new HashMap<>(), "testuser");

        boolean valid = jwtService.isTokenValid(token, "otheruser");

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValidReturnsFalseForExpiredToken() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "key", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", -3600000L); // Already expired

        String token = jwtService.generateToken(new HashMap<>(), "testuser");

        boolean valid = jwtService.isTokenValid(token, "testuser");

        assertThat(valid).isFalse();
    }

    @Test
    void tokenHasExpiredReturnsTrueForExpiredToken() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "key", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", -3600000L);

        String token = jwtService.generateToken(new HashMap<>(), "testuser");

        assertThat(jwtService.tokenHasExpired(token)).isTrue();
    }

    @Test
    void tokenHasExpiredReturnsFalseForValidToken() {
        String token = jwtService.generateToken(new HashMap<>(), "testuser");

        assertThat(jwtService.tokenHasExpired(token)).isFalse();
    }

    @Test
    void generateTokenWithDisplayNameCreatesTokenWithDisplayNameClaim() {
        var user = new com.kelvin.settlesense.domain.model.User();
        user.setDisplayName("John Doe");
        user.setEmail("john@example.com");

        String token = jwtService.generateTokenWithDisplayName(new HashMap<>(), user);

        assertThat(jwtService.extractDisplayName(token)).isEqualTo("John Doe");
        assertThat(jwtService.extractEmail(token)).isEqualTo("john@example.com");
        assertThat(jwtService.extractClaim(token, Claims::getSubject)).isEqualTo("john@example.com");
    }

    @Test
    void fetchTokenWithSubjectOnlyCreatesToken() {
        String token = jwtService.fetchToken("testuser");

        assertThat(token).isNotNull();
        String subject = jwtService.extractClaim(token, Claims::getSubject);
        assertThat(subject).isEqualTo("testuser");
    }

    @Test
    void fetchTokenWithClaimsAndSubjectCreatesToken() {
        HashMap<String, String> claims = new HashMap<>();
        claims.put("email", "test@example.com");

        String token = jwtService.fetchToken(claims, "testuser");

        String subject = jwtService.extractClaim(token, Claims::getSubject);
        assertThat(subject).isEqualTo("testuser");
        assertThat(jwtService.extractEmail(token)).isEqualTo("test@example.com");
    }
}
