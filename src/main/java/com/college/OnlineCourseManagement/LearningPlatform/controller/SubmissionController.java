package com.college.OnlineCourseManagement.LearningPlatform.controller;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.GradeSubmissionRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.SubmissionRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.SubmissionResponse;
import com.college.OnlineCourseManagement.LearningPlatform.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping("/api/student/assignments/{id}/submit")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submit(
            @PathVariable Long id,
            @Valid @ModelAttribute SubmissionRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        if (file != null && !file.isEmpty()) {
            request.setFile(file);
            request.setFileName(file.getOriginalFilename());
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.lastIndexOf('.') >= 0
                    ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase()
                    : "";
            request.setFileType(extension);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Assignment submitted successfully",
                        submissionService.submit(id, request)
                ));
    }

    @GetMapping("/api/student/submissions")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getStudentSubmissions() {
        return ResponseEntity.ok(ApiResponse.success(
                "Student submissions fetched successfully",
                submissionService.getStudentSubmissions()
        ));
    }

    @GetMapping("/api/instructor/assignments/{id}/submissions")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getSubmissions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Submissions fetched successfully",
                submissionService.getSubmissions(id)
        ));
    }

    @PutMapping("/api/instructor/submissions/{id}/grade")
    public ResponseEntity<ApiResponse<SubmissionResponse>> grade(
            @PathVariable Long id,
            @Valid @RequestBody GradeSubmissionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Submission graded successfully",
                submissionService.grade(id, request)
        ));
    }

    @GetMapping("/api/submissions/{id}/file")
    public ResponseEntity<org.springframework.core.io.Resource> getFile(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean download
    ) {
        return submissionService.getSubmissionFile(id, download);
    }
}