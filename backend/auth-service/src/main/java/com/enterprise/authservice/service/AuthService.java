package com.enterprise.authservice.service;

import com.enterprise.authservice.dto.request.*;
import com.enterprise.authservice.dto.response.*;
import com.enterprise.authservice.entity.*;
import com.enterprise.authservice.repository.RefreshTokenRepository;
import com.enterprise.authservice.repository.RoleRepository;
import com.enterprise.authservice.repository.UserRepository;
import com.enterprise.authservice.security.JwtService;
import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final AuditService auditService;

    @Transactional
    public UserSummaryResponse register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.USER_ALREADY_EXISTS, "An account with this email already exists");
        }

        RoleName assignedRole = RoleName.STUDENT;
        if (request.getRole() != null && request.getRole().equalsIgnoreCase("INSTRUCTOR")) {
            assignedRole = RoleName.INSTRUCTOR;
        }

        final RoleName finalRole = assignedRole;
        Role role = roleRepository.findByName(finalRole)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", finalRole));

        User user = User.builder()
                .email(email)
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.PENDING_VERIFICATION)
                .twoFactorEnabled(false)
                .failedLoginCount(0)
                .roles(new HashSet<>(Set.of(role)))
                .build();

        user = userRepository.save(user);

        // Generate email verification OTP and send dispatch
        String plainOtp = otpService.generateAndSaveOtp(user, email, OtpPurpose.EMAIL_VERIFICATION);
        emailService.sendVerificationEmail(email, plainOtp);

        auditService.logAction(user.getId(), assignedRole.name(), "USER_REGISTERED",
                "User", user.getId().toString(), null, null, null);

        return mapToSummary(user);
    }

    @Transactional
    public UserSummaryResponse verifyEmail(OtpVerifyRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        otpService.verifyOtp(email, OtpPurpose.EMAIL_VERIFICATION, request.getOtp());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());
        user = userRepository.save(user);

        auditService.logAction(user.getId(), "USER", "EMAIL_VERIFIED",
                "User", user.getId().toString(), null, null, null);

        return mapToSummary(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail().toLowerCase().trim();
        String clientIp = httpRequest != null ? httpRequest.getRemoteAddr() : null;
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        // Check account lock
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED,
                    "Account is temporarily locked until " + user.getLockedUntil() + " due to multiple failed login attempts");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(attempts);
            if (attempts >= 5) {
                user.setLockedUntil(Instant.now().plus(15, ChronoUnit.MINUTES));
                log.warn("Account locked for user {} after {} failed attempts", email, attempts);
            }
            userRepository.save(user);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Check verification and active status
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new ApiException(ErrorCode.ACCOUNT_NOT_VERIFIED, "Please verify your email before logging in");
        }
        if (user.getStatus() == UserStatus.SUSPENDED || user.getStatus() == UserStatus.DELETED) {
            throw new ApiException(ErrorCode.ACCOUNT_SUSPENDED, "This account is inactive or suspended");
        }

        // Reset lockout counters & record login
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Generate JWT Access Token
        String accessToken = jwtService.generateAccessToken(user);

        // Generate Refresh Token with Family Rotation
        String rawRefreshToken = jwtService.generateRawRefreshToken();
        String tokenHash = JwtService.hashToken(rawRefreshToken);
        UUID familyId = UUID.randomUUID();

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .familyId(familyId)
                .deviceInfo(request.getDeviceInfo() != null ? request.getDeviceInfo() : userAgent)
                .ipAddress(clientIp)
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()))
                .build();

        refreshTokenRepository.save(refreshToken);

        auditService.logAction(user.getId(), "USER", "USER_LOGGED_IN",
                "User", user.getId().toString(), clientIp, userAgent, null);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .user(mapToSummary(user))
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String rawToken = request.getRefreshToken();
        String tokenHash = JwtService.hashToken(rawToken);

        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID, "Invalid refresh token"));

        // Reuse detection: if token is already revoked, compromise detected! Revoke family.
        if (currentToken.isRevoked()) {
            log.warn("Security Alert: Refresh token reuse detected for family {}. Revoking all sessions in family.",
                    currentToken.getFamilyId());
            refreshTokenRepository.revokeFamily(currentToken.getFamilyId(), Instant.now());
            throw new ApiException(ErrorCode.TOKEN_REUSE_DETECTED);
        }

        if (currentToken.isExpired()) {
            throw new ApiException(ErrorCode.TOKEN_EXPIRED, "Refresh token expired. Please login again");
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(currentToken.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.ACCOUNT_SUSPENDED, "User account is not active");
        }

        // Rotate: generate new refresh token within the same family
        String newRawRefreshToken = jwtService.generateRawRefreshToken();
        String newTokenHash = JwtService.hashToken(newRawRefreshToken);

        UUID newId = UUID.randomUUID();
        RefreshToken newRefreshToken = RefreshToken.builder()
                .id(newId)
                .userId(user.getId())
                .tokenHash(newTokenHash)
                .familyId(currentToken.getFamilyId())
                .deviceInfo(request.getDeviceInfo() != null ? request.getDeviceInfo() : currentToken.getDeviceInfo())
                .ipAddress(httpRequest != null ? httpRequest.getRemoteAddr() : currentToken.getIpAddress())
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()))
                .build();

        currentToken.setRevokedAt(Instant.now());
        currentToken.setReplacedBy(newId);

        refreshTokenRepository.save(currentToken);
        refreshTokenRepository.save(newRefreshToken);

        String newAccessToken = jwtService.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .user(mapToSummary(user))
                .build();
    }

    @Transactional
    public void logout(UUID userId) {
        if (userId != null) {
            refreshTokenRepository.revokeAllUserTokens(userId, Instant.now());
        }
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        userRepository.findByEmail(email).ifPresent(user -> {
            String plainOtp = otpService.generateAndSaveOtp(user, email, OtpPurpose.PASSWORD_RESET);
            emailService.sendPasswordResetEmail(email, plainOtp);
            auditService.logAction(user.getId(), "USER", "FORGOT_PASSWORD_REQUESTED",
                    "User", user.getId().toString(), null, null, null);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        otpService.verifyOtp(email, OtpPurpose.PASSWORD_RESET, request.getOtp());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // Security requirement: Invalidate all sessions across devices upon password reset
        refreshTokenRepository.revokeAllUserTokens(user.getId(), Instant.now());

        auditService.logAction(user.getId(), "USER", "PASSWORD_RESET_SUCCESS",
                "User", user.getId().toString(), null, null, null);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Incorrect current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditService.logAction(user.getId(), "USER", "PASSWORD_CHANGED",
                "User", user.getId().toString(), null, null, null);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getUserSessions(UUID userId, String currentRefreshToken) {
        String currentHash = currentRefreshToken != null ? JwtService.hashToken(currentRefreshToken) : null;
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(userId);

        return tokens.stream().map(token -> SessionResponse.builder()
                .id(token.getId())
                .deviceInfo(token.getDeviceInfo())
                .ipAddress(token.getIpAddress())
                .createdAt(token.getCreatedAt())
                .expiresAt(token.getExpiresAt())
                .isCurrent(currentHash != null && currentHash.equals(token.getTokenHash()))
                .build()
        ).toList();
    }

    @Transactional
    public void revokeSession(UUID userId, UUID sessionId) {
        RefreshToken token = refreshTokenRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));

        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);

        auditService.logAction(userId, "USER", "SESSION_REVOKED",
                "RefreshToken", sessionId.toString(), null, null, null);
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUser(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToSummary(user);
    }

    private UserSummaryResponse mapToSummary(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .toList();

        return UserSummaryResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .roles(roles)
                .avatarUrl(null)
                .status(user.getStatus().name())
                .build();
    }
}
