package com.college.OnlineCourseManagement.LearningPlatform.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class InstructorDashboardResponse {
    List<InstructorCourseSummaryResponse> courses;
    long totalCourses;
    long totalEnrollments;
    BigDecimal averageProgress;
    long pendingSubmissions;
}
