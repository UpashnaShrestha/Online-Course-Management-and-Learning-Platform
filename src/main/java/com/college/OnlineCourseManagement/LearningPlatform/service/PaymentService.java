package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.PaymentCreateRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.PaymentProcessRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.PaymentResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Payment;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.NotificationType;
import com.college.OnlineCourseManagement.LearningPlatform.enums.PaymentStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.BadRequestException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CourseRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CourseRepository courseRepository;
    private final UserService userService;
    private final EnrollmentService enrollmentService;
    private final NotificationService notificationService;

    @Transactional
    public PaymentResponse createPendingPayment(PaymentCreateRequest request) {
        User student = userService.getAuthenticatedUser();
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can create payments");
        }

        Course course = courseRepository.findByIdAndStatus(request.getCourseId(), CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published course not found"));

        if (course.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Free courses do not require payment");
        }

        if (enrollmentService.isEnrolled(student.getId(), course.getId())) {
            throw new BadRequestException("You are already enrolled in this course");
        }

        Payment existingPayment = paymentRepository
                .findFirstByStudentIdAndCourseIdAndPaymentStatusOrderByCreatedAtDesc(
                        student.getId(), course.getId(), PaymentStatus.PENDING
                )
                .orElse(null);
        if (existingPayment != null) {
            return toResponse(existingPayment);
        }

        Payment payment = Payment.builder()
            .transactionId("DEMO-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .student(student)
                .course(course)
                .amount(course.getPrice())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)

                
                .build();

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse processPayment(Long id, PaymentProcessRequest request) {
        User student = userService.getAuthenticatedUser();
        Payment payment = paymentRepository.findByIdAndStudentId(id, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("This demo payment has already been processed");
        }

        // Record the dummy method the student selected on the checkout page.
        if (request.getPaymentMethod() != null) {
            payment.setPaymentMethod(request.getPaymentMethod());
        }

        if (!Boolean.TRUE.equals(request.getSuccessful())) {
            // Simulated decline: no enrollment is created.
            payment.setPaymentStatus(PaymentStatus.FAILED);
            return toResponse(paymentRepository.save(payment));
        }

        // Simulated gateway approval.
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Enrollment is created once, and only after the payment succeeds.
        Course course = payment.getCourse();
        if (!enrollmentService.isEnrolled(student.getId(), course.getId())) {
            enrollmentService.enrollPaidCourse(student, course);
        }

        notificationService.notify(
                student,
                "Payment successful",
                "We received Rs." + payment.getAmount().stripTrailingZeros().toPlainString()
                        + " for \"" + course.getTitle() + "\". Transaction "
                        + payment.getTransactionId() + ".",
                NotificationType.PAYMENT,
                "/payment/success/" + payment.getId()
        );

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getStudentPayments() {
        User student = userService.getAuthenticatedUser();
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can access payment history");
        }

        return paymentRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getStudentPayment(Long id) {
        User student = userService.getAuthenticatedUser();
        Payment payment = paymentRepository.findByIdAndStudentId(id, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAdminPayments() {
        requireAdmin();
        return paymentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getAdminRevenue() {
        requireAdmin();
        return paymentRepository.sumSuccessfulPayments();
    }

    @Transactional(readOnly = true)
    public BigDecimal getInstructorRevenue(Long courseId) {
        User instructor = userService.getAuthenticatedUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        if (instructor.getRole() != Role.ADMIN && (instructor.getRole() != Role.INSTRUCTOR
                || course.getInstructors().stream()
                .noneMatch(assigned -> assigned.getId().equals(instructor.getId())))) {
            throw new ForbiddenException("You are not allowed to view this course revenue");
        }
        return paymentRepository.sumSuccessfulPaymentsByCourseId(courseId);
    }

    private void requireAdmin() {
        if (userService.getAuthenticatedUser().getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can access payment reports");
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionId(payment.getTransactionId())
                .courseId(payment.getCourse().getId())
                .courseTitle(payment.getCourse().getTitle())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
