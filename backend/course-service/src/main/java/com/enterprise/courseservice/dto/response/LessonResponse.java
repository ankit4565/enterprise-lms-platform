package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.Lesson;
import com.enterprise.courseservice.entity.LessonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponse {

    private UUID id;
    private UUID moduleId;
    private UUID courseId;
    private String title;
    private LessonType type;
    private String content;
    private UUID mediaId;
    private Integer durationSeconds;
    private Integer position;
    private Boolean isFreePreview;
    private Boolean isPublished;
    private Instant createdAt;

    public static LessonResponse fromEntity(Lesson lesson) {
        if (lesson == null) return null;
        return LessonResponse.builder()
                .id(lesson.getId())
                .moduleId(lesson.getModule() != null ? lesson.getModule().getId() : null)
                .courseId(lesson.getCourseId())
                .title(lesson.getTitle())
                .type(lesson.getType())
                .content(lesson.getContent())
                .mediaId(lesson.getMediaId())
                .durationSeconds(lesson.getDurationSeconds())
                .position(lesson.getPosition())
                .isFreePreview(lesson.getIsFreePreview())
                .isPublished(lesson.getIsPublished())
                .createdAt(lesson.getCreatedAt())
                .build();
    }
}
