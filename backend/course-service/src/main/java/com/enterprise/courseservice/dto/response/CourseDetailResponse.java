package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.Course;
import com.enterprise.courseservice.entity.CourseLevel;
import com.enterprise.courseservice.entity.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDetailResponse {

    private UUID id;
    private UUID instructorId;
    private UUID categoryId;
    private String categoryName;
    private String categorySlug;
    private String title;
    private String slug;
    private String subtitle;
    private String description;
    private CourseLevel level;
    private String language;
    private String thumbnailUrl;
    private UUID promoVideoId;
    private Long priceMinor;
    private String currency;
    private Long discountPriceMinor;
    private CourseStatus status;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer enrolmentCount;
    private Integer totalDurationSeconds;
    private List<String> tags;
    private List<String> requirements;
    private List<String> outcomes;
    private Instant publishedAt;
    private String rejectedReason;
    private List<ModuleResponse> modules;
    private Instant createdAt;
    private Instant updatedAt;

    public static CourseDetailResponse fromEntity(Course course) {
        if (course == null) return null;
        return CourseDetailResponse.builder()
                .id(course.getId())
                .instructorId(course.getInstructorId())
                .categoryId(course.getCategory() != null ? course.getCategory().getId() : null)
                .categoryName(course.getCategory() != null ? course.getCategory().getName() : null)
                .categorySlug(course.getCategory() != null ? course.getCategory().getSlug() : null)
                .title(course.getTitle())
                .slug(course.getSlug())
                .subtitle(course.getSubtitle())
                .description(course.getDescription())
                .level(course.getLevel())
                .language(course.getLanguage())
                .thumbnailUrl(course.getThumbnailUrl())
                .promoVideoId(course.getPromoVideoId())
                .priceMinor(course.getPriceMinor())
                .currency(course.getCurrency())
                .discountPriceMinor(course.getDiscountPriceMinor())
                .status(course.getStatus())
                .ratingAvg(course.getRatingAvg())
                .ratingCount(course.getRatingCount())
                .enrolmentCount(course.getEnrolmentCount())
                .totalDurationSeconds(course.getTotalDurationSeconds())
                .tags(parseDelimitedList(course.getTags()))
                .requirements(parseDelimitedList(course.getRequirements()))
                .outcomes(parseDelimitedList(course.getOutcomes()))
                .publishedAt(course.getPublishedAt())
                .rejectedReason(course.getRejectedReason())
                .modules(course.getModules() != null ?
                        course.getModules().stream()
                                .map(ModuleResponse::fromEntity)
                                .collect(Collectors.toList()) : Collections.emptyList())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    private static List<String> parseDelimitedList(String str) {
        if (str == null || str.isBlank()) return Collections.emptyList();
        return Arrays.stream(str.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
