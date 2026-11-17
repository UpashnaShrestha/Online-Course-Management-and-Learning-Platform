package com.college.OnlineCourseManagement.LearningPlatform.dto;

import com.college.OnlineCourseManagement.LearningPlatform.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @StrongPassword
    private String newPassword;

    @NotBlank(message = "Please confirm your new password")
    private String confirmPassword;
}