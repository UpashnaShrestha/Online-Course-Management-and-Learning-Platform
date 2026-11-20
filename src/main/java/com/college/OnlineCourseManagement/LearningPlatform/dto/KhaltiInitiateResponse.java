package com.college.OnlineCourseManagement.LearningPlatform.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KhaltiInitiateResponse(
        String pidx,
        String payment_url,
        String expires_at,
        Integer expires_in
) {}
