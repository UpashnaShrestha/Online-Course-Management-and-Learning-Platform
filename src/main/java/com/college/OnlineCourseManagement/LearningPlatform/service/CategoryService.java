package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.CategoryRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.CategoryResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Category;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.DuplicateResourceException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CategoryRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserService userService;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CourseMapper::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAdminCategories() {
        if (userService.getAuthenticatedUser().getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can view category management");
        }

        return categoryRepository.findAll().stream()
                .map(category -> {
                    CategoryResponse response = CourseMapper.toCategoryResponse(category);
                    response.setCourses(courseRepository.findByCategoryIdWithDetails(category.getId()).stream()
                            .map(CourseMapper::toCategoryCourseResponse)
                            .toList());
                    return response;
                })
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (userService.getAuthenticatedUser().getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can create categories");
        }

        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Category name already exists");
        }

        Category category = Category.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .build();

        return CourseMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }
}
