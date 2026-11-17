package com.college.OnlineCourseManagement.LearningPlatform.controller;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.AssignmentRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.AssignmentResponse;
import com.college.OnlineCourseManagement.LearningPlatform.service.AssignmentService;
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
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping("/api/instructor/courses/{courseId}/assignments")
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @PathVariable Long courseId,
            @Valid @RequestBody AssignmentRequest request
    ) {
        AssignmentResponse assignment = assignmentService.createAssignment(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Assignment created successfully",
                        assignment
                ));
    }

    @GetMapping("/api/instructor/courses/{courseId}/assignments")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> getManagedAssignments(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Assignments fetched successfully",
                assignmentService.getManagedAssignments(courseId)
        ));
    }

    @PutMapping("/api/instructor/assignments/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Assignment updated successfully",
                assignmentService.updateAssignment(id, request)
        ));
    }

    @DeleteMapping("/api/instructor/assignments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.ok(ApiResponse.success("Assignment deleted successfully"));
    }

    @GetMapping("/api/courses/{courseId}/assignments")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> getAssignments(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Assignments fetched successfully",
                assignmentService.getCourseAssignments(courseId)
        ));
    }
}
