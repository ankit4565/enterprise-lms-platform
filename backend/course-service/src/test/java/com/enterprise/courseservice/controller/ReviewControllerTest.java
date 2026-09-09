package com.enterprise.courseservice.controller;

import com.enterprise.common.exception.GlobalExceptionHandler;
import com.enterprise.courseservice.dto.request.CreateReviewRequest;
import com.enterprise.courseservice.dto.response.ReviewResponse;
import com.enterprise.courseservice.security.UserPrincipal;
import com.enterprise.courseservice.service.ReviewService;
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
import org.springframework.data.domain.PageRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
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
    void getCourseReviews_Returns200() throws Exception {
        UUID courseId = UUID.randomUUID();
        ReviewResponse review = ReviewResponse.builder()
                .id(UUID.randomUUID())
                .courseId(courseId)
                .studentId(testStudentId)
                .rating(5)
                .comment("Excellent!")
                .isApproved(true)
                .build();

        Page<ReviewResponse> page = new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1);
        when(reviewService.getCourseReviews(eq(courseId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/courses/" + courseId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].rating").value(5));
    }

    @Test
    void createReview_Returns201() throws Exception {
        UUID courseId = UUID.randomUUID();
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(5)
                .comment("Incredible curriculum!")
                .build();

        ReviewResponse response = ReviewResponse.builder()
                .id(UUID.randomUUID())
                .courseId(courseId)
                .studentId(testStudentId)
                .rating(5)
                .comment("Incredible curriculum!")
                .isApproved(true)
                .build();

        when(reviewService.createReview(eq(courseId), eq(testStudentId), any(CreateReviewRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/courses/" + courseId + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rating").value(5));
    }
}
