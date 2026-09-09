package com.enterprise.courseservice.service;

import com.enterprise.common.exception.ResourceNotFoundException;
import com.enterprise.courseservice.dto.request.CreateCategoryRequest;
import com.enterprise.courseservice.dto.response.CategoryResponse;
import com.enterprise.courseservice.entity.Category;
import com.enterprise.courseservice.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private UUID categoryId;
    private Category category;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        category = Category.builder()
                .id(categoryId)
                .name("Computer Science")
                .slug("computer-science")
                .displayOrder(1)
                .isActive(true)
                .build();
    }

    @Test
    void getCategoryTree_ReturnsActiveTopLevelCategories() {
        when(categoryRepository.findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(category));

        List<CategoryResponse> result = categoryService.getCategoryTree();

        assertEquals(1, result.size());
        assertEquals("Computer Science", result.get(0).getName());
        assertEquals("computer-science", result.get(0).getSlug());
    }

    @Test
    void getCategoryBySlug_WhenFound_ReturnsCategoryResponse() {
        when(categoryRepository.findBySlug("computer-science")).thenReturn(Optional.of(category));

        CategoryResponse result = categoryService.getCategoryBySlug("computer-science");

        assertNotNull(result);
        assertEquals(categoryId, result.getId());
        assertEquals("computer-science", result.getSlug());
    }

    @Test
    void getCategoryBySlug_WhenNotFound_ThrowsResourceNotFoundException() {
        when(categoryRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryBySlug("nonexistent"));
    }

    @Test
    void createCategory_GeneratesUniqueSlugAndSaves() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Web Development")
                .icon("code-icon")
                .displayOrder(2)
                .build();

        when(categoryRepository.existsBySlug("web-development")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CategoryResponse result = categoryService.createCategory(request);

        assertNotNull(result);
        assertEquals("Web Development", result.getName());
        assertEquals("web-development", result.getSlug());
        assertEquals(2, result.getDisplayOrder());
        verify(categoryRepository).save(any(Category.class));
    }
}
