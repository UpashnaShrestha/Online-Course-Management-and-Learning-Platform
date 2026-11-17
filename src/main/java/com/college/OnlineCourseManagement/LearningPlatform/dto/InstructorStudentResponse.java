package com.college.OnlineCourseManagement.LearningPlatform.dto;

import com.college.OnlineCourseManagement.LearningPlatform.enums.EnrollmentStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class InstructorStudentResponse {
    Long studentId;
    String firstName;
    String lastName;
    String email;
    String username;
    LocalDateTime enrolledAt;
    BigDecimal progress;
    long completedLessons;
    long totalLessons;
    long submittedAssignments;
    long notSubmittedAssignments;
    long gradedAssignments;
    EnrollmentStatus courseStatus;
    LocalDateTime completedAt;
}
