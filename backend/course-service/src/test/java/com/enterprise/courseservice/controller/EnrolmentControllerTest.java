package com.enterprise.courseservice.controller;

import com.enterprise.common.exception.GlobalExceptionHandler;
import com.enterprise.courseservice.dto.response.EnrolmentResponse;
import com.enterprise.courseservice.entity.EnrolmentSource;
import com.enterprise.courseservice.entity.EnrolmentStatus;
import com.enterprise.courseservice.security.UserPrincipal;
import com.enterprise.courseservice.service.EnrolmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EnrolmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EnrolmentService enrolmentService;

    @InjectMocks
    private EnrolmentController enrolmentController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(enrolmentController)
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
    void enrol_Returns201() throws Exception {
        UUID courseId = UUID.randomUUID();
        EnrolmentResponse response = EnrolmentResponse.builder()
                .id(UUID.randomUUID())
                .courseId(courseId)
                .studentId(testStudentId)
                .status(EnrolmentStatus.ACTIVE)
                .source(EnrolmentSource.FREE)
                .progressPercent(BigDecimal.ZERO)
                .build();

        when(enrolmentService.enrolInCourse(eq(courseId), eq(testStudentId), eq(EnrolmentSource.FREE), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/courses/" + courseId + "/enrol"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void getMyEnrolments_Returns200() throws Exception {
        EnrolmentResponse response = EnrolmentResponse.builder()
                .id(UUID.randomUUID())
                .studentId(testStudentId)
                .status(EnrolmentStatus.ACTIVE)
                .build();

        Page<EnrolmentResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);
        when(enrolmentService.getMyEnrolments(eq(testStudentId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/courses/enrolments/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].studentId").value(testStudentId.toString()));
    }
}
