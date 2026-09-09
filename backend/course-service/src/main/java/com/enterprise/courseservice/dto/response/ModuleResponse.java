package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.Module;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleResponse {

    private UUID id;
    private UUID courseId;
    private String title;
    private String description;
    private Integer position;
    private List<LessonResponse> lessons;
    private Instant createdAt;

    public static ModuleResponse fromEntity(Module module) {
        if (module == null) return null;
        return ModuleResponse.builder()
                .id(module.getId())
                .courseId(module.getCourse() != null ? module.getCourse().getId() : null)
                .title(module.getTitle())
                .description(module.getDescription())
                .position(module.getPosition())
                .lessons(module.getLessons() != null ?
                        module.getLessons().stream()
                                .map(LessonResponse::fromEntity)
                                .collect(Collectors.toList()) : null)
                .createdAt(module.getCreatedAt())
                .build();
    }
}
