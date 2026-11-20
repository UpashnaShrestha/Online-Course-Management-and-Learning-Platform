package com.college.OnlineCourseManagement.LearningPlatform.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KhaltiLookupResponse(
        String pidx,
        Long total_amount,
        String status,
        String transaction_id,
        Integer fee,
        Boolean refunded
) {}
