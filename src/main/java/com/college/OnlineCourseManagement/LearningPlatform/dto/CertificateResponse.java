package com.college.OnlineCourseManagement.LearningPlatform.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class CertificateResponse {
    Long id;
    String certificateNumber;
    Long courseId;
    String courseTitle;
    LocalDateTime issuedAt;
}
