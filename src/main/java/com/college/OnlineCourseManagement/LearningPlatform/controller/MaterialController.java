package com.college.OnlineCourseManagement.LearningPlatform.controller;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.MaterialRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.MaterialResponse;
import com.college.OnlineCourseManagement.LearningPlatform.service.FileStorageService;
import com.college.OnlineCourseManagement.LearningPlatform.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final FileStorageService fileStorageService;

    @PostMapping("/api/instructor/lessons/{lessonId}/materials")
    public ResponseEntity<ApiResponse<MaterialResponse>> addMaterial(
            @PathVariable Long lessonId,
            @Valid @ModelAttribute MaterialRequest materialRequest,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        // If a file is provided, store it using FileStorageService
        if (file != null && !file.isEmpty()) {
            try {
                String storedFileName = fileStorageService.storeFile(file);
                materialRequest.setFileName(file.getOriginalFilename());
                materialRequest.setStoredFilePath(storedFileName);
                // Determine file type from extension
                String extension = storedFileName.substring(storedFileName.lastIndexOf('.') + 1).toLowerCase();
                materialRequest.setFileType(extension);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.failure("Failed to upload file: " + e.getMessage()));
            }
        }

        MaterialResponse material = materialService.addMaterial(lessonId, materialRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Material added successfully",
                        material
                ));
    }

    @PutMapping("/api/instructor/materials/{id}")
    public ResponseEntity<ApiResponse<MaterialResponse>> updateMaterial(
            @PathVariable Long id,
            @Valid @RequestBody MaterialRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Material updated successfully",
                materialService.updateMaterial(id, request)
        ));
    }

    @GetMapping("/api/lessons/{lessonId}/materials")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> getMaterials(@PathVariable Long lessonId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Materials fetched successfully",
                materialService.getMaterials(lessonId)
        ));
    }

    @GetMapping("/api/instructor/lessons/{lessonId}/materials")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> getManagedMaterials(@PathVariable Long lessonId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Instructor materials fetched successfully",
                materialService.getManagedMaterials(lessonId)
        ));
    }

    @DeleteMapping("/api/instructor/materials/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMaterial(@PathVariable Long id) {
        materialService.deleteMaterial(id);
        return ResponseEntity.ok(ApiResponse.success("Material deleted successfully"));
    }
}