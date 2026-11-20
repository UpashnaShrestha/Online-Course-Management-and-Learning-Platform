package com.college.OnlineCourseManagement.LearningPlatform.dto;

import java.util.Map;

public record KhaltiInitiateRequest(
        String return_url,
        String website_url,
        Long amount,
        String purchase_order_id,
        String purchase_order_name,
        Map<String, String> customer_info
) {}
