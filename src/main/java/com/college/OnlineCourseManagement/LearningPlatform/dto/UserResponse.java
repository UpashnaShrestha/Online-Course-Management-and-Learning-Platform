package com.college.OnlineCourseManagement.LearningPlatform.dto;

import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
    private LocalDate dateOfBirth;
    private Role role;
    private String profileImage;
    private String bio;
    private boolean enabled;
    private String professionalTitle;
    private String expertise;
    private String qualifications;
    private String teachingExperience;
    private boolean publicInstructorProfile;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
