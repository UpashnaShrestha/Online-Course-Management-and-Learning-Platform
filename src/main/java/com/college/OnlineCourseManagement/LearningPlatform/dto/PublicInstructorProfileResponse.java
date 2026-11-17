package com.college.OnlineCourseManagement.LearningPlatform.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PublicInstructorProfileResponse {
    Long id;
    String firstName;
    String lastName;
    String profileImage;
    String email;
    String professionalTitle;
    String bio;
    String expertise;
    String qualifications;
    String teachingExperience;
    List<PublicInstructorCourseResponse> courses;
}
