package com.enterprise.courseservice.dto.request;

import com.enterprise.courseservice.entity.CourseLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseRequest {

    @NotBlank(message = "Course title is required")
    @Size(max = 160, message = "Title cannot exceed 160 characters")
    private String title;

    @Size(max = 255, message = "Subtitle cannot exceed 255 characters")
    private String subtitle;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotBlank(message = "Description is required")
    private String description;

    private CourseLevel level;

    private String language;

    private String thumbnailUrl;

    private UUID promoVideoId;

    @PositiveOrZero(message = "Price cannot be negative")
    private Long priceMinor;

    private String currency;

    @PositiveOrZero(message = "Discount price cannot be negative")
    private Long discountPriceMinor;

    private List<String> tags;

    private List<String> requirements;

    private List<String> outcomes;
}
