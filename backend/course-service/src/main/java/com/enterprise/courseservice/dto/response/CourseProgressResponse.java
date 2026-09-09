package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.EnrolmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgressResponse {

    private UUID courseId;
    private String courseTitle;
    private UUID enrolmentId;
    private EnrolmentStatus enrolmentStatus;
    private BigDecimal progressPercent;
    private int completedLessonsCount;
    private int totalLessonsCount;
    private Instant completedAt;
    private Instant lastAccessedAt;
    private List<LessonProgressResponse> lessonProgresses;
}
