package com.college.OnlineCourseManagement.LearningPlatform.controller;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.EnrollmentResponse;
import com.college.OnlineCourseManagement.LearningPlatform.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enroll(@PathVariable Long courseId) {
        EnrollmentResponse enrollment = enrollmentService.enroll(courseId);
        return ResponseEntity.ok(ApiResponse.success(
                "Enrolled in course successfully",
                enrollment
        ));
    }

    @GetMapping("/student/enrollments")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getEnrollments() {
        List<EnrollmentResponse> enrollments = enrollmentService.getStudentEnrollments();
        return ResponseEntity.ok(ApiResponse.success(
                "Enrollments fetched successfully",
                enrollments
        ));
    }
}
