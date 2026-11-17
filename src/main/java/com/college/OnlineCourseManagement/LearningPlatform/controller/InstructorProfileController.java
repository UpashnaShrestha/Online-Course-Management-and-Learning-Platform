package com.college.OnlineCourseManagement.LearningPlatform.controller;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.PublicInstructorCourseResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.PublicInstructorProfileResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CourseRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructors")
@RequiredArgsConstructor
public class InstructorProfileController {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @GetMapping("/{id}/public-profile")
    public ResponseEntity<ApiResponse<PublicInstructorProfileResponse>> getPublicProfile(@PathVariable Long id) {
        User instructor = userRepository.findById(id)
                .filter(user -> user.getRole() == Role.INSTRUCTOR && user.isEnabled() && user.isPublicInstructorProfile())
                .orElseThrow(() -> new ResourceNotFoundException("Instructor profile not found"));

        PublicInstructorProfileResponse response = PublicInstructorProfileResponse.builder()
                .id(instructor.getId())
                .firstName(instructor.getFirstName())
                .lastName(instructor.getLastName())
                .profileImage(instructor.getProfileImage())
                .email(instructor.getEmail())
                .professionalTitle(instructor.getProfessionalTitle())
                .bio(instructor.getBio())
                .expertise(instructor.getExpertise())
                .qualifications(instructor.getQualifications())
                .teachingExperience(instructor.getTeachingExperience())
                .courses(courseRepository.findByInstructorIdWithDetails(instructor.getId()).stream()
                        .filter(course -> course.getStatus().name().equals("PUBLISHED"))
                        .map(course -> PublicInstructorCourseResponse.builder()
                                .id(course.getId()).title(course.getTitle()).thumbnail(course.getThumbnail()).build())
                        .toList())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Instructor profile fetched successfully", response));
    }
}
