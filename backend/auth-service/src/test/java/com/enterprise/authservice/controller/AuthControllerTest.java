package com.enterprise.authservice.controller;

import com.enterprise.authservice.dto.request.*;
import com.enterprise.authservice.dto.response.AuthResponse;
import com.enterprise.authservice.dto.response.UserSummaryResponse;
import com.enterprise.authservice.service.AuthService;
import com.enterprise.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_ValidPayload_Returns201AndStandardEnvelope() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Jane Doe")
                .email("jane@example.com")
                .password("StrongPass123")
                .build();

        UserSummaryResponse response = UserSummaryResponse.builder()
                .id(UUID.randomUUID())
                .fullName("Jane Doe")
                .email("jane@example.com")
                .roles(List.of("STUDENT"))
                .status("PENDING_VERIFICATION")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("jane@example.com"))
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void register_InvalidEmail_Returns400WithFieldErrors() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Jane Doe")
                .email("not-an-email")
                .password("StrongPass123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void login_ValidCredentials_Returns200WithTokens() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("jane@example.com")
                .password("StrongPass123")
                .build();

        UserSummaryResponse userSummary = UserSummaryResponse.builder()
                .id(UUID.randomUUID())
                .fullName("Jane Doe")
                .email("jane@example.com")
                .roles(List.of("STUDENT"))
                .status("ACTIVE")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .tokenType("Bearer")
                .expiresIn(900)
                .user(userSummary)
                .build();

        when(authService.login(any(LoginRequest.class), any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900));
    }

    @Test
    void forgotPassword_ValidEmail_Returns200Envelope() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("jane@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account exists with this email, a password reset code has been sent."));
    }

    @Test
    void resetPassword_ValidPayload_Returns200Envelope() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("jane@example.com")
                .otp("123456")
                .newPassword("NewPass1234")
                .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset successfully. Please log in with your new password."));
    }
}
