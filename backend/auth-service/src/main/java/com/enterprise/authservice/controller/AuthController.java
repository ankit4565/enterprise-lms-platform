package com.enterprise.authservice.controller;

import com.enterprise.authservice.dto.request.*;
import com.enterprise.authservice.dto.response.AuthResponse;
import com.enterprise.authservice.dto.response.SessionResponse;
import com.enterprise.authservice.dto.response.UserSummaryResponse;
import com.enterprise.authservice.service.AuthService;
import com.enterprise.common.dto.ApiResponse;
import com.enterprise.common.exception.UnauthorizedException;
import com.enterprise.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for registration, verification, login, password recovery, and session control")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user account")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        UserSummaryResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Account created successfully. Please check your email for the verification code."));
    }

    @Operation(summary = "Verify account email with 6-digit OTP code")
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> verifyEmail(
            @Valid @RequestBody OtpVerifyRequest request) {

        UserSummaryResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Email verified successfully. You may now log in."));
    }

    @Operation(summary = "Authenticate user and issue JWT tokens")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(response, "Login successful"));
    }

    @Operation(summary = "Rotate refresh token and obtain a new access token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse response = authService.refreshToken(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(response, "Token refreshed successfully"));
    }

    @Operation(summary = "Request a password reset OTP sent to registered email")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("If an account exists with this email, a password reset code has been sent."));
    }

    @Operation(summary = "Reset password using OTP code")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully. Please log in with your new password."));
    }

    @Operation(summary = "Change password for the authenticated user")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    @Operation(summary = "List all active refresh token sessions for the authenticated user")
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessions(
            @RequestHeader(value = "X-Refresh-Token", required = false) String currentRefreshToken) {

        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        List<SessionResponse> sessions = authService.getUserSessions(userId, currentRefreshToken);
        return ResponseEntity.ok(ApiResponse.ok(sessions, "Sessions fetched successfully"));
    }

    @Operation(summary = "Revoke a specific active device session")
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> revokeSession(@PathVariable UUID sessionId) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        authService.revokeSession(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.ok("Session revoked successfully"));
    }

    @Operation(summary = "Log out from the current device")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    @Operation(summary = "Log out and revoke all sessions across all devices")
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll() {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.ok("All sessions revoked successfully"));
    }

    @Operation(summary = "Get the authenticated user profile and roles")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getCurrentUser() {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        UserSummaryResponse response = authService.getCurrentUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(response, "User profile fetched successfully"));
    }
}
