package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.PaymentProcessRequest;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Category;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Payment;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.PaymentMethod;
import com.college.OnlineCourseManagement.LearningPlatform.enums.PaymentStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CourseRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserService userService;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private NotificationService notificationService;

    private PaymentService paymentService;

    private User student;
    private Course course;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository, courseRepository, userService, enrollmentService, notificationService);

        student = User.builder().id(1L).username("student").role(Role.STUDENT).build();

        course = Course.builder()
                .id(10L)
                .title("Java Programming Fundamentals")
                .price(new BigDecimal("500"))
                .status(CourseStatus.PUBLISHED)
                .category(Category.builder().id(1L).name("Programming").build())
                .build();

        when(userService.getAuthenticatedUser()).thenReturn(student);
    }

    private Payment pendingPayment() {
        return Payment.builder()
                .id(100L)
                .transactionId("DEMO-TXN-ABC12345")
                .student(student)
                .course(course)
                .amount(new BigDecimal("500"))
                .paymentMethod(PaymentMethod.DEMO)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("successful payment enrolls the student exactly once")
    void successEnrollsExactlyOnce() {
        Payment payment = pendingPayment();
        when(paymentRepository.findByIdAndStudentId(100L, 1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(enrollmentService.isEnrolled(1L, 10L)).thenReturn(false);

        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .successful(true)
                .paymentMethod(PaymentMethod.ESEWA)
                .build();

        var response = paymentService.processPayment(100L, request);

        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getPaidAt()).isNotNull();
        // The student's chosen dummy method is recorded.
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.ESEWA);

        // Regression: this used to run twice and blow up on the second insert.
        verify(enrollmentService, times(1)).enrollPaidCourse(student, course);
    }

    @Test
    @DisplayName("successful payment does not re-enroll an already enrolled student")
    void successDoesNotDuplicateEnrollment() {
        Payment payment = pendingPayment();
        when(paymentRepository.findByIdAndStudentId(100L, 1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(enrollmentService.isEnrolled(1L, 10L)).thenReturn(true);

        var response = paymentService.processPayment(100L,
                PaymentProcessRequest.builder().successful(true).build());

        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(enrollmentService, never()).enrollPaidCourse(any(), any());
    }

    @Test
    @DisplayName("a declined payment marks FAILED and creates no enrollment")
    void failureDoesNotEnroll() {
        Payment payment = pendingPayment();
        when(paymentRepository.findByIdAndStudentId(100L, 1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        var response = paymentService.processPayment(100L,
                PaymentProcessRequest.builder().successful(false).build());

        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.getPaidAt()).isNull();
        verify(enrollmentService, never()).enrollPaidCourse(any(), any());
    }
}