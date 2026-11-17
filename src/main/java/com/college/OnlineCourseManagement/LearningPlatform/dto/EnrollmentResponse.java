package com.college.OnlineCourseManagement.LearningPlatform.dto;

import com.college.OnlineCourseManagement.LearningPlatform.enums.EnrollmentStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class EnrollmentResponse {
    Long id;
    Long courseId;
    String courseTitle;
    EnrollmentStatus status;
    LocalDateTime enrolledAt;
    LocalDateTime completedAt;
    List<InstructorSummaryResponse> instructors;
}
