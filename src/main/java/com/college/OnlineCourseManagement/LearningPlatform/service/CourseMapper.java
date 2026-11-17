package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.CategoryResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.CategoryCourseResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.CourseResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.InstructorSummaryResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Category;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;

import java.util.Comparator;
import java.util.List;

public final class CourseMapper {

    private CourseMapper() {
    }

    public static CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }

    public static CategoryCourseResponse toCategoryCourseResponse(Course course) {
        List<InstructorSummaryResponse> instructors = course.getInstructors().stream()
                .sorted(Comparator.comparing(User::getUsername))
                .map(CourseMapper::toInstructorSummary)
                .toList();

        return CategoryCourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .price(course.getPrice())
                .level(course.getLevel())
                .duration(course.getDuration())
                .status(course.getStatus())
                .instructors(instructors)
                .build();
    }

    public static CourseResponse toResponse(Course course) {
        List<InstructorSummaryResponse> instructors = course.getInstructors().stream()
                .sorted(Comparator.comparing(User::getUsername))
                .map(CourseMapper::toInstructorSummary)
                .toList();

        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .price(course.getPrice())
                .thumbnail(course.getThumbnail())
                .level(course.getLevel())
                .duration(course.getDuration())
                .status(course.getStatus())
                .category(toCategoryResponse(course.getCategory()))
                .instructors(instructors)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    private static InstructorSummaryResponse toInstructorSummary(User user) {
        return InstructorSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profileImage(user.getProfileImage())
                .professionalTitle(user.getProfessionalTitle())
                .build();
    }
}
