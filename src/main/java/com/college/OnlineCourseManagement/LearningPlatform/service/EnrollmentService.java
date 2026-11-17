package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.EnrollmentResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.InstructorSummaryResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Enrollment;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.EnrollmentStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.NotificationType;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.BadRequestException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.DuplicateResourceException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CourseRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional
    public EnrollmentResponse enroll(Long courseId) {
        User student = userService.getAuthenticatedUser();
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can enroll in courses");
        }

        Course course = getPublishedCourse(courseId);
        if (course.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("This course requires payment. Complete checkout to enroll.");
        }
        if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), courseId)) {
            throw new DuplicateResourceException("You are already enrolled in this course");
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .build();

        EnrollmentResponse response = toResponse(enrollmentRepository.save(enrollment));
        notifyEnrollment(student, course);
        return response;
    }

    @Transactional
    public EnrollmentResponse enrollPaidCourse(User student, Course course) {
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can enroll in courses");
        }
        if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new DuplicateResourceException("You are already enrolled in this course");
        }
        EnrollmentResponse response = toResponse(
                enrollmentRepository.save(Enrollment.builder().student(student).course(course).build()));
        notifyEnrollment(student, course);
        return response;
    }

    private void notifyEnrollment(User student, Course course) {
        notificationService.notify(
                student,
                "Enrollment confirmed",
                "You are now enrolled in \"" + course.getTitle() + "\". Start learning any time.",
                NotificationType.ENROLLMENT,
                "/student/courses/" + course.getId() + "/learn"
        );
    }

    @Transactional(readOnly = true)
    public boolean isEnrolled(Long studentId, Long courseId) {
        return enrollmentRepository.existsByStudentIdAndCourseIdAndStatusNot(
                studentId, courseId, EnrollmentStatus.CANCELLED
        );
    }

    /**
     * Guards access to paid course content (lessons and materials).
     * Admins always pass, assigned instructors always pass, and students must have an
     * active (non-cancelled) enrollment for the course.
     */
    @Transactional(readOnly = true)
    public void assertCanViewCourseContent(Course course, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        if (user.getRole() == Role.INSTRUCTOR && course.getInstructors().stream()
                .anyMatch(instructor -> instructor.getId().equals(user.getId()))) {
            return;
        }

        if (user.getRole() == Role.STUDENT && enrollmentRepository
                .existsByStudentIdAndCourseIdAndStatusNot(
                        user.getId(), course.getId(), EnrollmentStatus.CANCELLED)) {
            return;
        }

        throw new ForbiddenException("You must be enrolled in this course to access its content");
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getStudentEnrollments() {
        User student = getStudent();
        return enrollmentRepository.findByStudentIdOrderByEnrolledAtDesc(student.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private User getStudent() {
        User student = userService.getAuthenticatedUser();
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can access enrollments");
        }
        return student;
    }

    private Course getPublishedCourse(Long courseId) {
        return courseRepository.findByIdAndStatus(courseId, CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published course not found"));
    }

    private EnrollmentResponse toResponse(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .courseId(enrollment.getCourse().getId())
                .courseTitle(enrollment.getCourse().getTitle())
                .status(enrollment.getStatus())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .instructors(enrollment.getCourse().getInstructors().stream()
                        .map(instructor -> InstructorSummaryResponse.builder()
                                .id(instructor.getId())
                                .username(instructor.getUsername())
                                .firstName(instructor.getFirstName())
                                .lastName(instructor.getLastName())
                                .profileImage(instructor.getProfileImage())
                                .professionalTitle(instructor.getProfessionalTitle())
                                .build())
                        .toList())
                .build();
    }
}
