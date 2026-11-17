package com.college.OnlineCourseManagement.LearningPlatform.controller;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.CourseProgressResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.LessonProgressResponse;
import com.college.OnlineCourseManagement.LearningPlatform.service.LessonProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LessonProgressController {

    private final LessonProgressService lessonProgressService;

    @PostMapping("/api/student/lessons/{id}/complete")
    public ResponseEntity<ApiResponse<LessonProgressResponse>> completeLesson(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lesson marked as completed",
                lessonProgressService.completeLesson(id)
        ));
    }

    @GetMapping("/api/student/courses/{id}/progress")
    public ResponseEntity<ApiResponse<CourseProgressResponse>> getCourseProgress(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Course progress fetched successfully",
                lessonProgressService.getCourseProgress(id)
        ));
    }
}
