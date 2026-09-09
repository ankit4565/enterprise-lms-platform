package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.Category;
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
public class CategoryResponse {

    private UUID id;
    private String name;
    private String slug;
    private UUID parentId;
    private String icon;
    private Integer displayOrder;
    private Boolean isActive;
    private List<CategoryResponse> subCategories;
    private Instant createdAt;

    public static CategoryResponse fromEntity(Category category) {
        if (category == null) return null;
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .icon(category.getIcon())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .subCategories(category.getSubCategories() != null ?
                        category.getSubCategories().stream()
                                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                                .map(CategoryResponse::fromEntity)
                                .collect(Collectors.toList()) : null)
                .createdAt(category.getCreatedAt())
                .build();
    }
}
