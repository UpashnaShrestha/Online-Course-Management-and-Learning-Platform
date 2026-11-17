package com.college.OnlineCourseManagement.LearningPlatform.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class LessonProgressResponse {
    Long id;
    Long lessonId;
    boolean completed;
    LocalDateTime completedAt;
}
