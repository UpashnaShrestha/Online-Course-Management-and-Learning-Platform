package com.college.OnlineCourseManagement.LearningPlatform.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import com.college.OnlineCourseManagement.LearningPlatform.enums.SubmissionMethod;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionRequest {

    /**
     * The assignment ID is typically passed via the URL path, not the DTO.
     * Kept for potential future use or explicit binding.
     */
    private Long assignmentId;

    private SubmissionMethod submissionMethod;

    /**
     * Uploaded file from the student's computer.
     * When provided, the file will be stored and its metadata (filename, path, type)
     * will be populated automatically.
     */
    private MultipartFile file;

    /**
     * External URL submission (e.g., GitHub repo, Google Drive doc).
     * Validated to be a well-formed URL.
     */
    @Pattern(regexp = "^(https?|ftp)://[^\s/$.?#].[^\s]*$", message = "URL must be a valid HTTP/HTTPS/FTP URL")
    private String url;

    /**
     * Text-based submission for written responses.
     * Limited to a reasonable size for text content.
     */
    @Size(max = 10000, message = "Text submission must not exceed 10000 characters")
    private String text;

    /**
     * Additional metadata as a map of key-value pairs.
     * Can be used for future extensibility (e.g., submission options, tags).
     */
    private Map<String, String> metadata;

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
     * The file type/extension of the uploaded file.
     * Populated from the uploaded file if using multipart upload.
     */
    @Size(max = 20, message = "File type must not exceed 20 characters")
    private String fileType;
}