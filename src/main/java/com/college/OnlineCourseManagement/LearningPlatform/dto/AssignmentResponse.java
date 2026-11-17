package com.college.OnlineCourseManagement.LearningPlatform.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Set;
import com.college.OnlineCourseManagement.LearningPlatform.enums.SubmissionMethod;

@Value
@Builder
public class AssignmentResponse {
    Long id;
    String title;
    String description;
    LocalDateTime dueDate;
    BigDecimal maxMarks;
    Set<SubmissionMethod> allowedSubmissionMethods;
    Set<String> allowedFileTypes;
    long maxFileSize;
    Long courseId;
    Long createdById;
    LocalDateTime createdAt;
}
