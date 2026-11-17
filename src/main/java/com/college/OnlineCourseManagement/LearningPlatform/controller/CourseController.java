package com.college.OnlineCourseManagement.LearningPlatform.controller;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.CategoryResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.CourseOptionResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.CourseResponse;
import com.college.OnlineCourseManagement.LearningPlatform.service.CategoryService;
import com.college.OnlineCourseManagement.LearningPlatform.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CategoryService categoryService;

    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getPublishedCourses() {
        List<CourseResponse> courses = courseService.getPublishedCourses();
        return ResponseEntity.ok(ApiResponse.success("Courses fetched successfully", courses));
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getPublishedCourse(@PathVariable Long id) {
        CourseResponse course = courseService.getPublishedCourseById(id);
        return ResponseEntity.ok(ApiResponse.success("Course fetched successfully", course));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("Categories fetched successfully", categories));
    }

    /**
     * The level and duration values a course may use. The instructor course form builds its
     * dropdowns from this, so the UI can never offer a value the backend would reject.
     */
    @GetMapping("/courses/options")
    public ResponseEntity<ApiResponse<Map<String, List<CourseOptionResponse>>>> getCourseOptions() {
        return ResponseEntity.ok(ApiResponse.success("Course options fetched successfully",
                courseService.getCourseOptions()));
    }
}
