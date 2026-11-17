package com.college.OnlineCourseManagement.LearningPlatform.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ModuleResponse {
    Long id;
    String title;
    String description;
    Integer orderNumber;
    Long courseId;
}
