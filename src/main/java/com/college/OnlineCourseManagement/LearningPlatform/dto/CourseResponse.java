package com.college.OnlineCourseManagement.LearningPlatform.dto;

import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String thumbnail;
    private String level;
    private String duration;
    private CourseStatus status;
    private CategoryResponse category;
    private List<InstructorSummaryResponse> instructors;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
