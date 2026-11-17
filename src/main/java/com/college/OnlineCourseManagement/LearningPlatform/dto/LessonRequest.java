package com.college.OnlineCourseManagement.LearningPlatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private String content;

    @Size(max = 500, message = "Video URL must not exceed 500 characters")
    private String videoUrl;

    @NotNull(message = "Order number is required")
    @Positive(message = "Order number must be greater than zero")
    private Integer orderNumber;
}
