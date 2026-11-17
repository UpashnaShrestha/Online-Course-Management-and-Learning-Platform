package com.college.OnlineCourseManagement.LearningPlatform.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class MaterialResponse {
    Long id;
    String title;
    String description;
    String fileName;
    String filePath;
    String fileType;
    Long lessonId;
    Long uploadedById;
    LocalDateTime uploadedAt;
}
