package com.enterprise.courseservice.controller;

import com.enterprise.common.exception.GlobalExceptionHandler;
import com.enterprise.courseservice.dto.request.ReviewCourseRequest;
import com.enterprise.courseservice.dto.response.CourseDetailResponse;
import com.enterprise.courseservice.dto.response.CourseSummaryResponse;
import com.enterprise.courseservice.entity.CourseStatus;
import com.enterprise.courseservice.service.CourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminCourseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private AdminCourseController adminCourseController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminCourseController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getPendingCourses_Returns200() throws Exception {
        CourseSummaryResponse summary = CourseSummaryResponse.builder()
                .id(UUID.randomUUID())
                .title("Pending Course")
                .status(CourseStatus.PENDING_REVIEW)
                .build();

        Page<CourseSummaryResponse> page = new PageImpl<>(List.of(summary), org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(courseService.getPendingCourses(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/courses/admin/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("Pending Course"));
    }

    @Test
    void reviewCourse_Approve_Returns200() throws Exception {
        UUID courseId = UUID.randomUUID();
        ReviewCourseRequest request = ReviewCourseRequest.builder()
                .decision("APPROVED")
                .build();

        CourseDetailResponse detail = CourseDetailResponse.builder()
                .id(courseId)
                .title("Pending Course")
                .status(CourseStatus.PUBLISHED)
                .build();

        when(courseService.reviewCourse(eq(courseId), any(ReviewCourseRequest.class))).thenReturn(detail);

        mockMvc.perform(post("/api/v1/courses/admin/" + courseId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }
}
