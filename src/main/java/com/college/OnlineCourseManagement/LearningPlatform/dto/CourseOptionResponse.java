package com.college.OnlineCourseManagement.LearningPlatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The level and duration options a course may declare.
 *
 * Exposed over HTTP so the instructor course form renders its dropdowns from the same
 * definition the backend validates against, instead of hardcoding the list in JavaScript
 * where the two can drift apart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseOptionResponse {

    private String value;
    private String label;

    /** Only set for durations: approximate length in hours, useful for sorting. */
    private Integer approximateHours;
}