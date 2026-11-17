package com.college.OnlineCourseManagement.LearningPlatform.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountDeletionRequest(
        @NotBlank(message = "A deletion reason is required")
        String reason
) {
}
