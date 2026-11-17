package com.college.OnlineCourseManagement.LearningPlatform.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeSubmissionRequest {

    @NotNull(message = "Grade is required")
    @DecimalMin(value = "0.0", message = "Grade must not be negative")
    @DecimalMax(value = "100.0", message = "Grade must not exceed 100")
    private BigDecimal grade;

    @Size(max = 5000, message = "Feedback must not exceed 5000 characters")
    private String feedback;
}
