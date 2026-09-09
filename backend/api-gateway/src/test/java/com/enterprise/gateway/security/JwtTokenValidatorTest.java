package com.enterprise.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenValidatorTest {

    private JwtTokenValidator validator;
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        validator = new JwtTokenValidator();
        ReflectionTestUtils.setField(validator, "jwtSecret", SECRET);
    }

    private String createTestToken(String userId, String email, List<String> roles, List<String> perms, long expiryMs) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("roles", roles)
                .claim("perms", perms)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        String token = createTestToken(UUID.randomUUID().toString(), "user@example.com", List.of("STUDENT"), List.of("COURSE_VIEW"), 60000);
        assertTrue(validator.validateToken(token));
    }

    @Test
    void validateToken_ExpiredToken_ReturnsFalse() {
        String token = createTestToken(UUID.randomUUID().toString(), "user@example.com", List.of("STUDENT"), List.of("COURSE_VIEW"), -1000);
        assertFalse(validator.validateToken(token));
    }

    @Test
    void validateToken_TamperedToken_ReturnsFalse() {
        String token = createTestToken(UUID.randomUUID().toString(), "user@example.com", List.of("STUDENT"), List.of("COURSE_VIEW"), 60000);
        String tampered = token + "xyz";
        assertFalse(validator.validateToken(tampered));
    }

    @Test
    void extractClaims_Success() {
        String userId = UUID.randomUUID().toString();
        String token = createTestToken(userId, "student@example.com", List.of("STUDENT"), List.of("COURSE_VIEW", "ENROL"), 60000);

        assertEquals(userId, validator.extractUserId(token));
        assertEquals("student@example.com", validator.extractEmail(token));
        assertEquals(List.of("STUDENT"), validator.extractRoles(token));
        assertEquals(List.of("COURSE_VIEW", "ENROL"), validator.extractPermissions(token));
    }
}
