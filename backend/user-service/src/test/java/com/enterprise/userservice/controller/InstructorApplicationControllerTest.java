package com.enterprise.userservice.controller;

import com.enterprise.common.exception.GlobalExceptionHandler;
import com.enterprise.userservice.dto.request.InstructorApplicationRequest;
import com.enterprise.userservice.dto.request.ReviewApplicationRequest;
import com.enterprise.userservice.dto.response.InstructorApplicationResponse;
import com.enterprise.userservice.entity.ApplicationStatus;
import com.enterprise.userservice.security.UserPrincipal;
import com.enterprise.userservice.service.InstructorApplicationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InstructorApplicationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InstructorApplicationService applicationService;

    @InjectMocks
    private InstructorApplicationController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID testUserId;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPrincipal = UserPrincipal.builder()
                .userId(testUserId)
                .email("instructor-candidate@example.com")
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

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void submitApplication_ValidPayload_Returns201AndEnvelope() throws Exception {
        InstructorApplicationRequest request = InstructorApplicationRequest.builder()
                .qualifications("10 years software development experience")
                .sampleUrl("https://youtube.com/lecture")
                .build();

        UUID appId = UUID.randomUUID();
        InstructorApplicationResponse response = InstructorApplicationResponse.builder()
                .id(appId)
                .userId(testUserId)
                .status(ApplicationStatus.PENDING)
                .qualifications("10 years software development experience")
                .sampleUrl("https://youtube.com/lecture")
                .createdAt(Instant.now())
                .build();

        when(applicationService.submitApplication(eq(testUserId), any(InstructorApplicationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/users/instructor-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(appId.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void submitApplication_MissingQualifications_Returns400() throws Exception {
        InstructorApplicationRequest request = InstructorApplicationRequest.builder()
                .qualifications("") // Blank
                .build();

        mockMvc.perform(post("/api/v1/users/instructor-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].field").value("qualifications"));
    }

    @Test
    void getMyApplications_Returns200AndList() throws Exception {
        UUID appId = UUID.randomUUID();
        InstructorApplicationResponse appResponse = InstructorApplicationResponse.builder()
                .id(appId)
                .userId(testUserId)
                .status(ApplicationStatus.PENDING)
                .qualifications("Senior Architect")
                .build();

        when(applicationService.getMyApplications(testUserId)).thenReturn(List.of(appResponse));

        mockMvc.perform(get("/api/v1/users/instructor-applications/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(appId.toString()));
    }

    @Test
    void reviewApplication_Returns200AndReviewedEnvelope() throws Exception {
        UUID appId = UUID.randomUUID();
        ReviewApplicationRequest request = ReviewApplicationRequest.builder()
                .status(ApplicationStatus.APPROVED)
                .reviewNote("Approved by Admin")
                .build();

        InstructorApplicationResponse reviewed = InstructorApplicationResponse.builder()
                .id(appId)
                .userId(UUID.randomUUID())
                .status(ApplicationStatus.APPROVED)
                .reviewedBy(testUserId)
                .reviewNote("Approved by Admin")
                .reviewedAt(Instant.now())
                .build();

        when(applicationService.reviewApplication(eq(appId), eq(testUserId), any(ReviewApplicationRequest.class)))
                .thenReturn(reviewed);

        mockMvc.perform(put("/api/v1/users/instructor-applications/" + appId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewNote").value("Approved by Admin"));
    }
}
