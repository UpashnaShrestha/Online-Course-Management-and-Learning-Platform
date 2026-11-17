package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.UserResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .dateOfBirth(user.getDateOfBirth())
                .role(user.getRole())
                .profileImage(user.getProfileImage())
                .bio(user.getBio())
                .enabled(user.isEnabled())
                .professionalTitle(user.getProfessionalTitle())
                .expertise(user.getExpertise())
                .qualifications(user.getQualifications())
                .teachingExperience(user.getTeachingExperience())
                .publicInstructorProfile(user.isPublicInstructorProfile())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
