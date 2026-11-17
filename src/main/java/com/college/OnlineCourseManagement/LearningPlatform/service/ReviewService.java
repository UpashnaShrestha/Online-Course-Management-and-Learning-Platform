package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ReviewRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.ReviewResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Review;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.EnrollmentStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.DuplicateResourceException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CourseRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.EnrollmentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserService userService;

    @Transactional
    public ReviewResponse createReview(Long courseId, ReviewRequest request) {
        User student = userService.getAuthenticatedUser();
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can review courses");
        }

        Course course = getPublishedCourse(courseId);
        if (!enrollmentRepository.existsByStudentIdAndCourseIdAndStatusNot(
            student.getId(), courseId, EnrollmentStatus.CANCELLED
        )) {
            throw new ForbiddenException("You must be enrolled in the course to review it");
        }
        if (reviewRepository.existsByStudentIdAndCourseId(student.getId(), courseId)) {
            throw new DuplicateResourceException("You have already reviewed this course");
        }

        Review review = Review.builder()
                .student(student)
                .course(course)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        return toResponse(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getCourseReviews(Long courseId) {
        getPublishedCourse(courseId);
        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .map(this::toResponse)
                .toList();
    }

    private Course getPublishedCourse(Long courseId) {
        return courseRepository.findByIdAndStatus(courseId, CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published course not found"));
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .courseId(review.getCourse().getId())
                .studentId(review.getStudent().getId())
                .studentUsername(review.getStudent().getUsername())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
