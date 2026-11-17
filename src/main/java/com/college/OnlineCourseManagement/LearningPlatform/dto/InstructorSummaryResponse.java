package com.college.OnlineCourseManagement.LearningPlatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorSummaryResponse {

    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String profileImage;
    private String professionalTitle;
}
