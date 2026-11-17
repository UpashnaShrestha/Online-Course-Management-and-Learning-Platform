package com.college.OnlineCourseManagement.LearningPlatform.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PublicInstructorCourseResponse {
    Long id;
    String title;
    String thumbnail;
}
