package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.entity.Category;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Payment;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.PaymentMethod;
import com.college.OnlineCourseManagement.LearningPlatform.enums.PaymentStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.BadRequestException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CourseRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserService userService;
    @Mock private EnrollmentService enrollmentService;
    @Mock private NotificationService notificationService;

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

    private Payment khaltiPayment(PaymentStatus status) {
        return Payment.builder()
                .id(100L)
                .khaltiPidx("pidx-test")
                .purchaseOrderId("OCMS-test")
                .transactionId(status == PaymentStatus.SUCCESS ? "txn-test" : null)
                .student(student)
                .course(course)
                .amount(new BigDecimal("500"))
                .paymentMethod(PaymentMethod.KHALTI)
                .paymentStatus(status)
                .paidAt(status == PaymentStatus.SUCCESS ? LocalDateTime.now() : null)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("completed payment returns without reprocessing or re-enrolling")
    void completedPaymentIsIdempotent() {
        Payment payment = khaltiPayment(PaymentStatus.SUCCESS);
        when(paymentRepository.findByKhaltiPidx("pidx-test")).thenReturn(Optional.of(payment));
        when(paymentRepository.findByIdAndStudentId(100L, 1L)).thenReturn(Optional.of(payment));

        var response = paymentService.verifyByPidx("pidx-test", "OCMS-test", "Completed");

        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository, never()).save(org.mockito.ArgumentMatchers.any(Payment.class));
        verify(enrollmentService, never()).enrollPaidCourse(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("purchase order mismatch is rejected before lookup")
    void purchaseOrderMismatchIsRejected() {
        Payment payment = khaltiPayment(PaymentStatus.PENDING);
        when(paymentRepository.findByKhaltiPidx("pidx-test")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.verifyByPidx("pidx-test", "wrong-order", "Completed"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("another student cannot verify a payment")
    void anotherStudentCannotVerifyPayment() {
        User other = User.builder().id(2L).username("other").role(Role.STUDENT).build();
        Payment payment = khaltiPayment(PaymentStatus.PENDING);
        payment.setStudent(other);
        when(paymentRepository.findByKhaltiPidx("pidx-test")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.verifyByPidx("pidx-test", "OCMS-test", "Completed"))
                .isInstanceOf(ForbiddenException.class);
    }
}
