package org.tampavolunteers.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies W-3: every generated token carries a unique jti claim.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Provide a 256-bit+ secret (W-2 enforces this at startup via env var)
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-secret-for-unit-tests-minimum-256-bits-abcdefghijklmnop");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3_600_000L);
    }

    private UserDetails user(String username) {
        return new User(username, "password", Collections.emptyList());
    }

    @Test
    void generatedToken_containsJtiClaim() {
        String token = jwtUtil.generateToken(user("alice@example.com"));
        String jti = jwtUtil.extractJti(token);
        assertThat(jti).isNotNull().isNotBlank();
    }

    @Test
    void eachToken_hasUniqueJti() {
        UserDetails u = user("alice@example.com");
        String jti1 = jwtUtil.extractJti(jwtUtil.generateToken(u));
        String jti2 = jwtUtil.extractJti(jwtUtil.generateToken(u));
        assertThat(jti1).isNotEqualTo(jti2);
    }

    @Test
    void validateToken_returnsTrueForValidToken() {
        UserDetails u = user("alice@example.com");
        String token = jwtUtil.generateToken(u);
        assertThat(jwtUtil.validateToken(token, u)).isTrue();
    }

    @Test
    void extractUsername_returnsCorrectSubject() {
        UserDetails u = user("bob@example.com");
        String token = jwtUtil.generateToken(u);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("bob@example.com");
    }
}
