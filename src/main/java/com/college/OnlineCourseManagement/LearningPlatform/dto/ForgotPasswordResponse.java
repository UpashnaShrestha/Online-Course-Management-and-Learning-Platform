package com.college.OnlineCourseManagement.LearningPlatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordResponse {

    /**
     * Only populated when app.password-reset.expose-otp is enabled, which exists so the
     * flow can be used without a working mail server. MUST stay false in production.
     */
    private String otp;
}