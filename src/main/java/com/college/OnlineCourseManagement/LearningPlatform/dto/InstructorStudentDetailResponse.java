package com.college.OnlineCourseManagement.LearningPlatform.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class InstructorStudentDetailResponse {
    Long studentId;
    String firstName;
    String lastName;
    String email;
    String username;
    String phoneNumber;
    String bio;
    List<InstructorStudentCourseResponse> courses;
}
