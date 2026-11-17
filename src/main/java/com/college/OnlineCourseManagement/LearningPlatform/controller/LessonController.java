package com.college.OnlineCourseManagement.LearningPlatform.controller;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.LessonRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.LessonResponse;
import com.college.OnlineCourseManagement.LearningPlatform.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @GetMapping("/api/instructor/modules/{moduleId}/lessons")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> getManagedLessons(@PathVariable Long moduleId) {
        return ResponseEntity.ok(ApiResponse.success("Instructor lessons fetched successfully", lessonService.getManagedLessons(moduleId)));
    }

    @PostMapping("/api/instructor/modules/{moduleId}/lessons")
    public ResponseEntity<ApiResponse<LessonResponse>> createLesson(
            @PathVariable Long moduleId,
            @Valid @RequestBody LessonRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lesson created successfully", lessonService.createLesson(moduleId, request)));
    }

    @GetMapping("/api/modules/{moduleId}/lessons")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> getLessons(@PathVariable Long moduleId) {
        return ResponseEntity.ok(ApiResponse.success("Lessons fetched successfully", lessonService.getLessons(moduleId)));
    }

    @PutMapping("/api/instructor/lessons/{id}")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(
            @PathVariable Long id,
            @Valid @RequestBody LessonRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Lesson updated successfully", lessonService.updateLesson(id, request)));
    }

    @DeleteMapping("/api/instructor/lessons/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(@PathVariable Long id) {
        lessonService.deleteLesson(id);
        return ResponseEntity.ok(ApiResponse.success("Lesson deleted successfully"));
    }
}
