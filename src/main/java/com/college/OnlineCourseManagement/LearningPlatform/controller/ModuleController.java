package com.college.OnlineCourseManagement.LearningPlatform.controller;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.ModuleRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.ModuleResponse;
import com.college.OnlineCourseManagement.LearningPlatform.service.ModuleService;
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
public class ModuleController {

    private final ModuleService moduleService;

    @GetMapping("/api/instructor/courses/{courseId}/modules")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> getManagedModules(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Instructor modules fetched successfully",
                moduleService.getManagedModules(courseId)
        ));
    }

    @PostMapping("/api/instructor/courses/{courseId}/modules")
    public ResponseEntity<ApiResponse<ModuleResponse>> createModule(
            @PathVariable Long courseId,
            @Valid @RequestBody ModuleRequest request
    ) {
        ModuleResponse module = moduleService.createModule(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Module created successfully", module));
    }

    @GetMapping("/api/courses/{courseId}/modules")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> getModules(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Modules fetched successfully",
                moduleService.getPublishedModules(courseId)
        ));
    }

    @PutMapping("/api/instructor/modules/{id}")
    public ResponseEntity<ApiResponse<ModuleResponse>> updateModule(
            @PathVariable Long id,
            @Valid @RequestBody ModuleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Module updated successfully",
                moduleService.updateModule(id, request)
        ));
    }

    @DeleteMapping("/api/instructor/modules/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteModule(@PathVariable Long id) {
        moduleService.deleteModule(id);
        return ResponseEntity.ok(ApiResponse.success("Module deleted successfully"));
    }
}
