package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.entity.Category;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.BadRequestException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CourseRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    private EnrollmentService enrollmentService;

    private User student;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(
                enrollmentRepository, courseRepository, userService, notificationService);

        student = User.builder()
                .id(1L)
                .username("student")
                .role(Role.STUDENT)
                .build();
    }

    private Course publishedCourse(BigDecimal price) {
        return Course.builder()
                .id(10L)
                .title("Java Programming Fundamentals")
                .price(price)
                .status(CourseStatus.PUBLISHED)
                .category(Category.builder().id(1L).name("Programming").build())
                .build();
    }

    @Test
    @DisplayName("enroll() must reject paid courses so the payment flow cannot be bypassed")
    void enrollRejectsPaidCourse() {
        Course paid = publishedCourse(new BigDecimal("500"));
        when(userService.getAuthenticatedUser()).thenReturn(student);
        when(courseRepository.findByIdAndStatus(10L, CourseStatus.PUBLISHED)).thenReturn(Optional.of(paid));

        assertThatThrownBy(() -> enrollmentService.enroll(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("requires payment");

        // Critically: no enrollment row may be written.
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("enroll() must still allow free courses")
    void enrollAllowsFreeCourse() {
        Course free = publishedCourse(BigDecimal.ZERO);
        when(userService.getAuthenticatedUser()).thenReturn(student);
        when(courseRepository.findByIdAndStatus(10L, CourseStatus.PUBLISHED)).thenReturn(Optional.of(free));
        when(enrollmentRepository.existsByStudentIdAndCourseId(1L, 10L)).thenReturn(false);
        when(enrollmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> enrollmentService.enroll(10L)).doesNotThrowAnyException();

        verify(enrollmentRepository).save(any());
    }

    @Test
    @DisplayName("enroll() must reject non-students")
    void enrollRejectsNonStudents() {
        User instructor = User.builder().id(2L).username("teacher").role(Role.INSTRUCTOR).build();
        when(userService.getAuthenticatedUser()).thenReturn(instructor);

        assertThatThrownBy(() -> enrollmentService.enroll(10L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("assertCanViewCourseContent denies students who are not enrolled")
    void contentAccessDeniedForNonEnrolledStudent() {
        Course paid = publishedCourse(new BigDecimal("500"));
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatusNot(
                any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> enrollmentService.assertCanViewCourseContent(paid, student))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("must be enrolled");
    }

    @Test
    @DisplayName("assertCanViewCourseContent allows an enrolled student")
    void contentAccessAllowedForEnrolledStudent() {
        Course paid = publishedCourse(new BigDecimal("500"));
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatusNot(
                any(), any(), any())).thenReturn(true);

        assertThatCode(() -> enrollmentService.assertCanViewCourseContent(paid, student))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertCanViewCourseContent always allows an admin")
    void contentAccessAllowedForAdmin() {
        Course paid = publishedCourse(new BigDecimal("500"));
        User admin = User.builder().id(3L).username("root").role(Role.ADMIN).build();

        assertThatCode(() -> enrollmentService.assertCanViewCourseContent(paid, admin))
                .doesNotThrowAnyException();
    }
}