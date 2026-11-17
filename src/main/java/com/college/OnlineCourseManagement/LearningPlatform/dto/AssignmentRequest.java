package com.college.OnlineCourseManagement.LearningPlatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Set;
import com.college.OnlineCourseManagement.LearningPlatform.enums.SubmissionMethod;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Due date is required")
    private LocalDateTime dueDate;

    @NotNull(message = "Maximum marks are required")
    @DecimalMin(value = "0.01", message = "Maximum marks must be greater than zero")
    @DecimalMax(value = "999.99", message = "Maximum marks must not exceed 999.99")
    private BigDecimal maxMarks;

    @NotNull(message = "At least one submission method is required")
    @Size(min = 1, message = "At least one submission method is required")
    private Set<SubmissionMethod> allowedSubmissionMethods;

    private Set<String> allowedFileTypes;

    @DecimalMin(value = "1", message = "Maximum file size must be positive")
    private Long maxFileSize;
}
