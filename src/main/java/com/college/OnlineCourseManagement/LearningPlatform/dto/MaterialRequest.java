package com.college.OnlineCourseManagement.LearningPlatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    /**
     * The original filename as provided by the user (before storage).
     * Populated from the uploaded file if using multipart upload.
     */
    @Size(max = 255, message = "File name must not exceed 255 characters")
    private String fileName;

    /**
     * The stored filename after processing (unique name to prevent conflicts).
     * Populated from the uploaded file if using multipart upload.
     */
    @Size(max = 500, message = "Stored file path must not exceed 500 characters")
    private String storedFilePath;

    /**
     * The MIME type of the uploaded file.
     * Populated from the uploaded file if using multipart upload.
     */
    @Size(max = 100, message = "File type must not exceed 100 characters")
    private String fileType;

    /**
     * The actual uploaded file. Used for multipart file uploads.
     * When using this constructor or form submission, the file will be stored
     * and fileName/storedFilePath/fileType will be auto-populated.
     */
    private MultipartFile file;
}