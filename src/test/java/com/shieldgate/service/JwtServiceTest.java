package com.shieldgate.service;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "super-secure-test-secret-key-that-is-at-least-256-bits-long!!";
    private final long expiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, expiration);
    }

    @Test
    void testGenerateAndValidateToken() {
        String username = "testuser";
        String token = jwtService.generateToken(username);

        assertNotNull(token);
        assertFalse(token.isBlank());

        String extractedUsername = jwtService.validateToken(token);
        assertEquals(username, extractedUsername);
    }

    @Test
    void testValidateInvalidTokenThrowsException() {
        String invalidToken = "invalid.jwt.token";
        assertThrows(JwtException.class, () -> jwtService.validateToken(invalidToken));
    }

    @Test
    void testExpiredTokenThrowsException() throws InterruptedException {
        // 1 ms expiration for immediate expiry test
        JwtService shortLivedJwtService = new JwtService(secret, 1);
        String token = shortLivedJwtService.generateToken("user1");

        Thread.sleep(10);

        assertThrows(JwtException.class, () -> shortLivedJwtService.validateToken(token));
    }
}
