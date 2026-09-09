package com.enterprise.courseservice.controller;

import com.enterprise.common.exception.GlobalExceptionHandler;
import com.enterprise.courseservice.dto.request.CreateCourseRequest;
import com.enterprise.courseservice.dto.request.UpdateCourseRequest;
import com.enterprise.courseservice.dto.response.CourseDetailResponse;
import com.enterprise.courseservice.dto.response.CourseSummaryResponse;
import com.enterprise.courseservice.entity.CourseStatus;
import com.enterprise.courseservice.security.UserPrincipal;
import com.enterprise.courseservice.service.CourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private CourseController courseController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID testInstructorId;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        testInstructorId = UUID.randomUUID();
        testPrincipal = UserPrincipal.builder()
                .userId(testInstructorId)
                .email("instructor@example.com")
                .roles(List.of("INSTRUCTOR"))
                .permissions(Collections.emptyList())
                .authorities(Collections.emptyList())
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(courseController)
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
    void searchCatalogue_Returns200() throws Exception {
        CourseSummaryResponse summary = CourseSummaryResponse.builder()
                .id(UUID.randomUUID())
                .title("Complete Java")
                .slug("complete-java")
                .status(CourseStatus.PUBLISHED)
                .build();

        Page<CourseSummaryResponse> page = new PageImpl<>(List.of(summary), org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(courseService.searchCatalogue(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].slug").value("complete-java"));
    }

    @Test
    void getCourseDetail_Returns200() throws Exception {
        UUID courseId = UUID.randomUUID();
        CourseDetailResponse detail = CourseDetailResponse.builder()
                .id(courseId)
                .title("Complete Java")
                .slug("complete-java")
                .status(CourseStatus.PUBLISHED)
                .build();

        when(courseService.getCourseDetail(eq("complete-java"), any(), any())).thenReturn(detail);

        mockMvc.perform(get("/api/v1/courses/complete-java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Complete Java"));
    }

    @Test
    void createCourse_Returns201() throws Exception {
        UUID courseId = UUID.randomUUID();
        CreateCourseRequest request = CreateCourseRequest.builder()
                .title("Complete Java")
                .categoryId(UUID.randomUUID())
                .description("A detailed comprehensive course on Java programming language.")
                .build();

        CourseDetailResponse detail = CourseDetailResponse.builder()
                .id(courseId)
                .title("Complete Java")
                .slug("complete-java")
                .status(CourseStatus.DRAFT)
                .build();

        when(courseService.createCourse(eq(testInstructorId), any(CreateCourseRequest.class)))
                .thenReturn(detail);

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("complete-java"));
    }

    @Test
    void submitCourse_Returns200() throws Exception {
        UUID courseId = UUID.randomUUID();
        CourseDetailResponse detail = CourseDetailResponse.builder()
                .id(courseId)
                .title("Complete Java")
                .status(CourseStatus.PENDING_REVIEW)
                .build();

        when(courseService.submitCourseForReview(eq(courseId), eq(testInstructorId), any()))
                .thenReturn(detail);

        mockMvc.perform(post("/api/v1/courses/" + courseId + "/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));
    }
}
