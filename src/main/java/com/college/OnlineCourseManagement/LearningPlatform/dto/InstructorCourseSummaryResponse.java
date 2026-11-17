package com.college.OnlineCourseManagement.LearningPlatform.dto;

import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Value
@Builder
public class InstructorCourseSummaryResponse {
    Long id;
    String title;
    String thumbnail;
    BigDecimal price;
    CourseStatus status;
    LocalDateTime createdAt;
    long enrolledStudents;
    long completedStudents;
    long totalLessons;
    long totalAssignments;
    BigDecimal averageProgress;
}
