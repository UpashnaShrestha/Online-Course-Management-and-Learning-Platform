package com.college.OnlineCourseManagement.LearningPlatform.dto;

import com.college.OnlineCourseManagement.LearningPlatform.enums.SubmissionStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.college.OnlineCourseManagement.LearningPlatform.enums.SubmissionMethod;

@Value
@Builder
public class SubmissionResponse {
    Long id;
    Long assignmentId;
    Long studentId;
    String studentName;
    String studentUsername;
    String filePath;
    String fileName;
    String fileType;
    SubmissionMethod submissionMethod;
    String textContent;
    int attemptNumber;
    LocalDateTime submittedAt;
    SubmissionStatus status;
    BigDecimal grade;
    String feedback;
    LocalDateTime gradedAt;
}
