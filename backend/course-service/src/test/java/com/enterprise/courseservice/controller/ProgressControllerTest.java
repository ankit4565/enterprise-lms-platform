package com.enterprise.courseservice.controller;

import com.enterprise.common.exception.GlobalExceptionHandler;
import com.enterprise.courseservice.dto.request.UpdateProgressRequest;
import com.enterprise.courseservice.dto.response.CourseProgressResponse;
import com.enterprise.courseservice.dto.response.LessonProgressResponse;
import com.enterprise.courseservice.entity.EnrolmentStatus;
import com.enterprise.courseservice.entity.ProgressStatus;
import com.enterprise.courseservice.security.UserPrincipal;
import com.enterprise.courseservice.service.LessonProgressService;
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

import java.math.BigDecimal;
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
class ProgressControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LessonProgressService lessonProgressService;

    @InjectMocks
    private ProgressController progressController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID testStudentId;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        testStudentId = UUID.randomUUID();
        testPrincipal = UserPrincipal.builder()
                .userId(testStudentId)
                .email("student@example.com")
                .roles(List.of("STUDENT"))
                .permissions(Collections.emptyList())
                .authorities(Collections.emptyList())
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(progressController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return testPrincipal;
                    }
                })
                .build();
    }

    @Test
    void getCourseProgress_Returns200() throws Exception {
        UUID courseId = UUID.randomUUID();
        CourseProgressResponse response = CourseProgressResponse.builder()
                .courseId(courseId)
                .enrolmentStatus(EnrolmentStatus.ACTIVE)
                .progressPercent(BigDecimal.valueOf(50.00))
                .completedLessonsCount(1)
                .totalLessonsCount(2)
                .build();

        when(lessonProgressService.getCourseProgress(courseId, testStudentId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/courses/" + courseId + "/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.progressPercent").value(50.0));
    }

    @Test
    void updateLessonProgress_Returns200() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        UpdateProgressRequest request = UpdateProgressRequest.builder()
                .watchedSeconds(120)
                .lastPositionSeconds(120)
                .build();

        LessonProgressResponse response = LessonProgressResponse.builder()
                .id(UUID.randomUUID())
                .lessonId(lessonId)
                .status(ProgressStatus.IN_PROGRESS)
                .watchedSeconds(120)
                .lastPositionSeconds(120)
                .build();

        when(lessonProgressService.recordProgress(eq(courseId), eq(lessonId), eq(testStudentId), any(UpdateProgressRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/courses/" + courseId + "/lessons/" + lessonId + "/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }
}
