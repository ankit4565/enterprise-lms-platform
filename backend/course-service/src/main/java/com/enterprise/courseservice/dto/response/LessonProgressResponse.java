package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.LessonProgress;
import com.enterprise.courseservice.entity.ProgressStatus;
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
public class LessonProgressResponse {

    private UUID id;
    private UUID lessonId;
    private String lessonTitle;
    private ProgressStatus status;
    private Integer lastPositionSeconds;
    private Integer watchedSeconds;
    private Instant completedAt;

    public static LessonProgressResponse fromEntity(LessonProgress progress) {
        if (progress == null) return null;
        return LessonProgressResponse.builder()
                .id(progress.getId())
                .lessonId(progress.getLesson() != null ? progress.getLesson().getId() : null)
                .lessonTitle(progress.getLesson() != null ? progress.getLesson().getTitle() : null)
                .status(progress.getStatus())
                .lastPositionSeconds(progress.getLastPositionSeconds())
                .watchedSeconds(progress.getWatchedSeconds())
                .completedAt(progress.getCompletedAt())
                .build();
    }
}
