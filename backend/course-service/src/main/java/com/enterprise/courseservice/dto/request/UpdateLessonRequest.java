package com.enterprise.courseservice.dto.request;

import com.enterprise.courseservice.entity.LessonType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLessonRequest {

    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    private LessonType type;

    private String content;

    private UUID mediaId;

    private Integer durationSeconds;

    private Boolean isFreePreview;

    private Boolean isPublished;
}
