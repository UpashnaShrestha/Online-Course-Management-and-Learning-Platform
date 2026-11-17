package com.college.OnlineCourseManagement.LearningPlatform.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class CourseProgressResponse {
    Long courseId;
    long completedLessons;
    long totalLessons;
    BigDecimal percentage;
    /** Lets the client restore per-lesson completion state after a reload. */
    List<Long> completedLessonIds;
}
