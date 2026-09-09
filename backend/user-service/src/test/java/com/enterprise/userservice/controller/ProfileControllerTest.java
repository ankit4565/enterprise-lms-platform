package com.enterprise.userservice.controller;

import com.enterprise.common.exception.GlobalExceptionHandler;
import com.enterprise.userservice.dto.request.UpdateProfileRequest;
import com.enterprise.userservice.dto.response.ProfileResponse;
import com.enterprise.userservice.security.UserPrincipal;
import com.enterprise.userservice.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfileController profileController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID testUserId;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPrincipal = UserPrincipal.builder()
                .userId(testUserId)
                .email("learner@example.com")
                .roles(List.of("STUDENT"))
                .permissions(Collections.emptyList())
                .authorities(Collections.emptyList())
                .build();

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return testPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(profileController)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getMyProfile_Returns200AndProfileEnvelope() throws Exception {
        ProfileResponse response = ProfileResponse.builder()
                .userId(testUserId)
                .headline("Java Architect")
                .bio("Loves microservices")
                .isPublic(true)
                .createdAt(Instant.now())
                .build();

        when(profileService.getOrCreateProfile(testUserId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/profile/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.data.headline").value("Java Architect"));
    }

    @Test
    void updateMyProfile_Returns200AndUpdatedEnvelope() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .headline("Principal Architect")
                .country("India")
                .build();

        ProfileResponse response = ProfileResponse.builder()
                .userId(testUserId)
                .headline("Principal Architect")
                .country("India")
                .isPublic(true)
                .build();

        when(profileService.updateProfile(eq(testUserId), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.headline").value("Principal Architect"))
                .andExpect(jsonPath("$.data.country").value("India"));
    }

    @Test
    void getPublicProfile_Returns200AndProfileEnvelope() throws Exception {
        UUID targetId = UUID.randomUUID();
        ProfileResponse response = ProfileResponse.builder()
                .userId(targetId)
                .headline("Instructor Profile")
                .isPublic(true)
                .build();

        when(profileService.getPublicProfile(eq(targetId), eq(testUserId), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/profile/" + targetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(targetId.toString()))
                .andExpect(jsonPath("$.data.headline").value("Instructor Profile"));
    }
}
