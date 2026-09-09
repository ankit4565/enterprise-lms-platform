package com.enterprise.courseservice.controller;

import com.enterprise.common.exception.GlobalExceptionHandler;
import com.enterprise.courseservice.dto.request.CreateAssignmentRequest;
import com.enterprise.courseservice.dto.request.GradeSubmissionRequest;
import com.enterprise.courseservice.dto.request.SubmitAssignmentRequest;
import com.enterprise.courseservice.dto.response.AssignmentResponse;
import com.enterprise.courseservice.dto.response.SubmissionResponse;
import com.enterprise.courseservice.entity.SubmissionStatus;
import com.enterprise.courseservice.security.UserPrincipal;
import com.enterprise.courseservice.service.AssignmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AssignmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AssignmentService assignmentService;

    @InjectMocks
    private AssignmentController assignmentController;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private UUID testUserId;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPrincipal = UserPrincipal.builder()
                .userId(testUserId)
                .email("instructor@example.com")
                .roles(List.of("INSTRUCTOR"))
                .permissions(Collections.emptyList())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(assignmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                            }

                            @Override
                            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                                return testPrincipal;
                            }
                        },
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    void createAssignment_Returns201() throws Exception {
        UUID lessonId = UUID.randomUUID();
        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .title("Build a Microservice")
                .instructions("Complete the exercise")
                .maxScore(100)
                .build();

        AssignmentResponse response = AssignmentResponse.builder()
                .id(UUID.randomUUID())
                .lessonId(lessonId)
                .title("Build a Microservice")
                .maxScore(100)
                .build();

        when(assignmentService.createAssignment(eq(lessonId), eq(testUserId), any(CreateAssignmentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/lessons/" + lessonId + "/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Build a Microservice"));
    }

    @Test
    void getAssignment_Returns200() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        AssignmentResponse response = AssignmentResponse.builder()
                .id(assignmentId)
                .title("Database Design Project")
                .maxScore(100)
                .build();

        when(assignmentService.getAssignment(eq(assignmentId), eq(testUserId), eq(true)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/assignments/" + assignmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Database Design Project"));
    }

    @Test
    void submitAssignment_Returns201() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        SubmitAssignmentRequest request = SubmitAssignmentRequest.builder()
                .textAnswer("My answer content")
                .fileUrls("http://storage/solution.zip")
                .build();

        SubmissionResponse response = SubmissionResponse.builder()
                .id(UUID.randomUUID())
                .assignmentId(assignmentId)
                .studentId(testUserId)
                .attemptNo(1)
                .status(SubmissionStatus.SUBMITTED)
                .build();

        when(assignmentService.submitAssignment(eq(assignmentId), eq(testUserId), any(SubmitAssignmentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/assignments/" + assignmentId + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.attemptNo").value(1));
    }

    @Test
    void getMySubmission_Returns200() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        SubmissionResponse response = SubmissionResponse.builder()
                .id(UUID.randomUUID())
                .assignmentId(assignmentId)
                .studentId(testUserId)
                .attemptNo(1)
                .status(SubmissionStatus.SUBMITTED)
                .build();

        when(assignmentService.getMySubmission(eq(assignmentId), eq(testUserId)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/assignments/" + assignmentId + "/submissions/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.attemptNo").value(1));
    }

    @Test
    void getGradingQueue_Returns200() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        SubmissionResponse sub = SubmissionResponse.builder()
                .id(UUID.randomUUID())
                .assignmentId(assignmentId)
                .status(SubmissionStatus.SUBMITTED)
                .build();

        Page<SubmissionResponse> page = new PageImpl<>(List.of(sub));
        when(assignmentService.getGradingQueue(eq(assignmentId), eq(testUserId), eq(null), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/assignments/" + assignmentId + "/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].status").value("SUBMITTED"));
    }

    @Test
    void gradeSubmission_Returns200() throws Exception {
        UUID submissionId = UUID.randomUUID();
        GradeSubmissionRequest request = GradeSubmissionRequest.builder()
                .score(new BigDecimal("95.00"))
                .feedback("Excellent work!")
                .status(SubmissionStatus.GRADED)
                .build();

        SubmissionResponse response = SubmissionResponse.builder()
                .id(submissionId)
                .finalScore(new BigDecimal("95.00"))
                .status(SubmissionStatus.GRADED)
                .feedback("Excellent work!")
                .build();

        when(assignmentService.gradeSubmission(eq(submissionId), eq(testUserId), any(GradeSubmissionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/assignments/submissions/" + submissionId + "/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.finalScore").value(95.00))
                .andExpect(jsonPath("$.data.status").value("GRADED"));
    }
}
