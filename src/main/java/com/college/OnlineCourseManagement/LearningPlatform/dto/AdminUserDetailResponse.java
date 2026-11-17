package com.college.OnlineCourseManagement.LearningPlatform.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminUserDetailResponse(
        UserResponse user,
        StudentActivity student,
        InstructorActivity instructor
) {
    public record StudentActivity(
            List<Enrollment> enrollments,
            LearningSummary learningSummary,
            List<AssignmentActivity> assignments,
            List<Payment> payments,
            List<Certificate> certificates
    ) {
        public record Enrollment(
                Long courseId,
                String courseTitle,
                List<String> instructors,
                BigDecimal progress,
                LocalDateTime enrolledAt,
                LocalDateTime completedAt
        ) {}

        public record Submission(
                Long id,
                String assignmentTitle,
                String courseTitle,
                String status,
                BigDecimal grade,
                String feedback,
                LocalDateTime submittedAt
        ) {}

        public record AssignmentActivity(
                Long assignmentId,
                String assignmentTitle,
                String courseTitle,
                String status,
                BigDecimal grade,
                String feedback,
                LocalDateTime dueDate,
                LocalDateTime submittedAt
        ) {}

        public record LearningSummary(
                long completedLessons,
                long completedCourses,
                long certificatesEarned
        ) {}

        public record Payment(
                Long id,
                String courseTitle,
                BigDecimal amount,
                String status,
                String transactionId,
                LocalDateTime createdAt,
                LocalDateTime paidAt
        ) {}

        public record Certificate(
                Long id,
                String courseTitle,
                String certificateNumber,
                LocalDateTime issuedAt
        ) {}
    }

    public record InstructorActivity(
            List<Course> courses,
            long totalAssignments,
            long totalEnrolledStudents,
            long submissionsGraded,
            long publishedCourses,
            long pendingCourses,
            long rejectedCourses,
            BigDecimal averageStudentProgress
    ) {
        public record Course(
                Long id,
                String title,
                String status,
                long enrolledStudents,
                long assignments,
                LocalDateTime createdAt
        ) {}
    }
}
