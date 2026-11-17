package com.college.OnlineCourseManagement.LearningPlatform.controller;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.CategoryRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.CategoryResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.CourseResponse;
import com.college.OnlineCourseManagement.LearningPlatform.service.CategoryService;
import com.college.OnlineCourseManagement.LearningPlatform.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseService courseService;
    private final CategoryService categoryService;

    @PostMapping("/api/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", category));
    }

    @GetMapping("/api/admin/courses/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getPendingCourses() {
        List<CourseResponse> courses = courseService.getPendingCourses();
        return ResponseEntity.ok(ApiResponse.success("Pending courses fetched successfully", courses));
    }

    @GetMapping("/api/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAdminCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                "Categories with courses fetched successfully",
                categoryService.getAdminCategories()
        ));
    }

    @PutMapping("/api/admin/courses/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> approveCourse(@PathVariable Long id) {
        CourseResponse course = courseService.approveCourse(id);
        return ResponseEntity.ok(ApiResponse.success("Course approved successfully", course));
    }

    @PutMapping("/api/admin/courses/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> rejectCourse(@PathVariable Long id) {
        CourseResponse course = courseService.rejectCourse(id);
        return ResponseEntity.ok(ApiResponse.success("Course rejected successfully", course));
    }
}
