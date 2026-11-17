package com.college.OnlineCourseManagement.LearningPlatform.dto;

import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseDuration;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseLevel;
import com.college.OnlineCourseManagement.LearningPlatform.validation.OneOf;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be zero or greater")
    // Matches entity precision 10 / scale 2 (max 99,999,999.99). Without this an oversized
    // price passes validation and fails at flush as an opaque constraint violation.
    @Digits(integer = 8, fraction = 2, message = "Price must be at most 99,999,999.99")
    private BigDecimal price;

    @Size(max = 500, message = "Thumbnail URL must not exceed 500 characters")
    private String thumbnail;

    /**
     * Offered as a dropdown in the UI. Validated against the closed CourseLevel set so a
     * hand-rolled API client cannot introduce "beginner" alongside "BEGINNER".
     */
    @Size(max = 50, message = "Level must not exceed 50 characters")
    @OneOf(value = CourseLevel.class, message = "Level must be Beginner, Intermediate or Advanced")
    private String level;

    /** Offered as a dropdown in the UI. Validated against the closed CourseDuration set. */
    @Size(max = 100, message = "Duration must not exceed 100 characters")
    @OneOf(value = CourseDuration.class, message = "Duration must be one of the offered options")
    private String duration;

    @NotNull(message = "Category id is required")
    private Long categoryId;

    private Set<Long> instructorIds;
}
