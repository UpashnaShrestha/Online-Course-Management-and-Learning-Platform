package com.college.OnlineCourseManagement.LearningPlatform.dto;

import com.college.OnlineCourseManagement.LearningPlatform.enums.EnrollmentStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class InstructorStudentCourseResponse {
    Long courseId;
    String courseTitle;
    BigDecimal progress;
    long completedLessons;
    long totalLessons;
    EnrollmentStatus status;
    LocalDateTime enrolledAt;
    LocalDateTime completedAt;
    List<SubmissionResponse> submissions;
    boolean certificateIssued;
}
