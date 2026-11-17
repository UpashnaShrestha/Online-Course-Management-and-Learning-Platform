package com.college.OnlineCourseManagement.LearningPlatform.controller;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.InstructorDashboardResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.InstructorStudentDetailResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.InstructorStudentResponse;
import com.college.OnlineCourseManagement.LearningPlatform.service.InstructorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/instructor/dashboard")
@RequiredArgsConstructor
public class InstructorDashboardController {

    private final InstructorDashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<InstructorDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Instructor dashboard fetched successfully", dashboardService.getDashboard()));
    }

    @GetMapping("/courses/{courseId}/students")
    public ResponseEntity<ApiResponse<List<InstructorStudentResponse>>> getStudents(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.success("Course students fetched successfully", dashboardService.getStudents(courseId)));
    }

    @GetMapping("/courses/{courseId}/students/{studentId}")
    public ResponseEntity<ApiResponse<InstructorStudentDetailResponse>> getStudent(
            @PathVariable Long courseId,
            @PathVariable Long studentId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Student progress fetched successfully", dashboardService.getStudent(courseId, studentId)));
    }
}
