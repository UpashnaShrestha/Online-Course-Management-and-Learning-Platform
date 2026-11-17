package com.college.OnlineCourseManagement.LearningPlatform.dto;

import com.college.OnlineCourseManagement.LearningPlatform.service.OtpService;
import com.college.OnlineCourseManagement.LearningPlatform.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Reset code is required")
    // Exactly as many digits as OtpService generates. A looser pattern would accept codes that
    // can never match a stored hash, producing a confusing "invalid or expired" error.
    @Pattern(regexp = OtpService.REGEX, message = "Enter the 6-digit code from your email")
    private String otp;

    @NotBlank(message = "New password is required")
    @StrongPassword
    private String newPassword;

    @NotBlank(message = "Please confirm your new password")
    private String confirmPassword;
}