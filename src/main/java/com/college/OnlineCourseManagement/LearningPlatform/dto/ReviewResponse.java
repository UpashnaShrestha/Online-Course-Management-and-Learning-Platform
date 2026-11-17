package com.college.OnlineCourseManagement.LearningPlatform.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ReviewResponse {
    Long id;
    Long courseId;
    Long studentId;
    String studentUsername;
    Integer rating;
    String comment;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
