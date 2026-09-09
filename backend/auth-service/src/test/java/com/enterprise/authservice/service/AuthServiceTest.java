package com.enterprise.authservice.service;

import com.enterprise.authservice.dto.request.*;
import com.enterprise.authservice.dto.response.AuthResponse;
import com.enterprise.authservice.dto.response.SessionResponse;
import com.enterprise.authservice.dto.response.UserSummaryResponse;
import com.enterprise.authservice.entity.*;
import com.enterprise.authservice.repository.RefreshTokenRepository;
import com.enterprise.authservice.repository.RoleRepository;
import com.enterprise.authservice.repository.UserRepository;
import com.enterprise.authservice.security.JwtService;
import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private Role studentRole;

    @BeforeEach
    void setUp() {
        studentRole = Role.builder()
                .id(UUID.randomUUID())
                .name(RoleName.STUDENT)
                .build();

        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hashed_password")
                .fullName("Test User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(studentRole))
                .failedLoginCount(0)
                .build();
    }

    @Test
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("Password123!")
                .role("STUDENT")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(otpService.generateAndSaveOtp(any(), anyString(), any())).thenReturn("123456");

        UserSummaryResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertEquals(UserStatus.PENDING_VERIFICATION.name(), response.getStatus());

        verify(emailService).sendVerificationEmail(eq("test@example.com"), eq("123456"));
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("Password123!")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(request));
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void verifyEmail_Success() {
        sampleUser.setStatus(UserStatus.PENDING_VERIFICATION);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        OtpVerifyRequest request = OtpVerifyRequest.builder()
                .email("test@example.com")
                .otp("123456")
                .build();

        UserSummaryResponse response = authService.verifyEmail(request);

        assertEquals(UserStatus.ACTIVE.name(), response.getStatus());
        verify(otpService).verifyOtp(eq("test@example.com"), eq(OtpPurpose.EMAIL_VERIFICATION), eq("123456"));
    }

    @Test
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123!", "hashed_password")).thenReturn(true);
        when(jwtService.generateAccessToken(sampleUser)).thenReturn("access.jwt.token");
        when(jwtService.generateRawRefreshToken()).thenReturn("raw-refresh-token");
        when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthResponse response = authService.login(request, null);

        assertNotNull(response);
        assertEquals("access.jwt.token", response.getAccessToken());
        assertEquals("raw-refresh-token", response.getRefreshToken());
        assertEquals(900L, response.getExpiresIn());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_WrongPassword_ThrowsExceptionAndIncrementsCounter() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("WrongPassword")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword", "hashed_password")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request, null));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        assertEquals(1, sampleUser.getFailedLoginCount());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void refreshToken_ReuseDetected_RevokesFamily() {
        UUID familyId = UUID.randomUUID();
        RefreshToken revokedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(sampleUser.getId())
                .familyId(familyId)
                .tokenHash(JwtService.hashToken("reused-token"))
                .revokedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revokedToken));

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("reused-token")
                .build();

        ApiException ex = assertThrows(ApiException.class, () -> authService.refreshToken(request, null));
        assertEquals(ErrorCode.TOKEN_REUSE_DETECTED, ex.getErrorCode());
        verify(refreshTokenRepository).revokeFamily(eq(familyId), any(Instant.class));
    }

    @Test
    void forgotPassword_ExistingUser_SendsResetOtp() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(otpService.generateAndSaveOtp(sampleUser, "test@example.com", OtpPurpose.PASSWORD_RESET))
                .thenReturn("654321");

        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("test@example.com")
                .build();

        authService.forgotPassword(request);

        verify(emailService).sendPasswordResetEmail("test@example.com", "654321");
    }

    @Test
    void resetPassword_Success_UpdatesPasswordAndRevokesSessions() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode("NewStrongPass123")).thenReturn("new_hashed_password");

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("test@example.com")
                .otp("654321")
                .newPassword("NewStrongPass123")
                .build();

        authService.resetPassword(request);

        assertEquals("new_hashed_password", sampleUser.getPasswordHash());
        verify(otpService).verifyOtp("test@example.com", OtpPurpose.PASSWORD_RESET, "654321");
        verify(refreshTokenRepository).revokeAllUserTokens(eq(sampleUser.getId()), any(Instant.class));
        verify(userRepository).save(sampleUser);
    }

    @Test
    void changePassword_ValidOldPassword_UpdatesPassword() {
        when(userRepository.findByIdAndDeletedAtIsNull(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("OldPassword123", "hashed_password")).thenReturn(true);
        when(passwordEncoder.encode("BrandNewPass123")).thenReturn("brand_new_hash");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("OldPassword123")
                .newPassword("BrandNewPass123")
                .build();

        authService.changePassword(sampleUser.getId(), request);

        assertEquals("brand_new_hash", sampleUser.getPasswordHash());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void changePassword_WrongOldPassword_ThrowsException() {
        when(userRepository.findByIdAndDeletedAtIsNull(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongOldPassword", "hashed_password")).thenReturn(false);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("WrongOldPassword")
                .newPassword("BrandNewPass123")
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                authService.changePassword(sampleUser.getId(), request));

        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserSessions_ReturnsActiveSessions() {
        RefreshToken token1 = RefreshToken.builder()
                .id(UUID.randomUUID())
                .deviceInfo("Chrome Mac")
                .ipAddress("127.0.0.1")
                .tokenHash(JwtService.hashToken("token-1"))
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(sampleUser.getId()))
                .thenReturn(java.util.List.of(token1));

        java.util.List<SessionResponse> sessions = authService.getUserSessions(sampleUser.getId(), "token-1");

        assertEquals(1, sessions.size());
        assertEquals("Chrome Mac", sessions.get(0).getDeviceInfo());
        assertTrue(sessions.get(0).isCurrent());
    }
}
