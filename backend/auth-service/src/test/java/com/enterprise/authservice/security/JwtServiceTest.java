package com.enterprise.authservice.security;

import com.enterprise.authservice.entity.Permission;
import com.enterprise.authservice.entity.Role;
import com.enterprise.authservice.entity.RoleName;
import com.enterprise.authservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", 900000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationMs", 604800000L);
        ReflectionTestUtils.setField(jwtService, "issuer", "lms-auth");
        ReflectionTestUtils.setField(jwtService, "audience", "lms-api");
    }

    @Test
    void testGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        Permission permission = Permission.builder()
                .id(UUID.randomUUID())
                .code("COURSE_VIEW")
                .description("View courses")
                .build();

        Role role = Role.builder()
                .id(UUID.randomUUID())
                .name(RoleName.STUDENT)
                .permissions(Set.of(permission))
                .build();

        User user = User.builder()
                .id(userId)
                .email("student@example.com")
                .fullName("John Doe")
                .roles(Set.of(role))
                .build();

        String token = jwtService.generateAccessToken(user);
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));

        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals("student@example.com", jwtService.extractEmail(token));

        List<String> roles = jwtService.extractRoles(token);
        assertTrue(roles.contains("STUDENT"));

        List<String> permissions = jwtService.extractPermissions(token);
        assertTrue(permissions.contains("COURSE_VIEW"));
    }

    @Test
    void testTokenHashIsDeterministic() {
        String rawToken = "sample-raw-token-value-12345";
        String hash1 = JwtService.hashToken(rawToken);
        String hash2 = JwtService.hashToken(rawToken);

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length()); // SHA-256 produces 64 hex characters
    }
}
