package com.college.OnlineCourseManagement.LearningPlatform.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LessonResponse {
    Long id;
    String title;
    String description;
    String content;
    String videoUrl;
    Integer orderNumber;
    Long moduleId;
}
