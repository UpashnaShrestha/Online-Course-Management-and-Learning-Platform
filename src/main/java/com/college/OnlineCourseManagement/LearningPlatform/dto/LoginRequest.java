package com.college.OnlineCourseManagement.LearningPlatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    /**
     * When true the refresh token is issued with the longer "remember me" lifetime so the
     * session survives closing the browser.
     */
    private boolean rememberMe;
}