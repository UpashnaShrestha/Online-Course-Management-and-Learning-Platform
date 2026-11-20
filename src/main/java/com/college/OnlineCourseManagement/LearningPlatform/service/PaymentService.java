package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.KhaltiInitiateRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.KhaltiInitiateResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.KhaltiLookupResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.PaymentCreateRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.PaymentResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Payment;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.NotificationType;
import com.college.OnlineCourseManagement.LearningPlatform.enums.PaymentMethod;
import com.college.OnlineCourseManagement.LearningPlatform.enums.PaymentStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.BadRequestException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CourseRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CourseRepository courseRepository;
    private final UserService userService;
    private final EnrollmentService enrollmentService;
    private final NotificationService notificationService;

    @Value("${khalti.base-url:https://dev.khalti.com/api/v2}")
    private String khaltiApiBaseUrl;

    @Value("${khalti.secret-key:}")
    private String khaltiSecretKey;

    @Value("${khalti.return-url:http://localhost:5173/payment/khalti/callback}")
    private String khaltiReturnUrl;

    @Value("${khalti.website-url:http://localhost:5173}")
    private String khaltiWebsiteUrl;

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
        if (khaltiSecretKey == null || khaltiSecretKey.isBlank()) {
            throw new BadRequestException("Khalti Sandbox is not configured");
        }

        if (enrollmentService.isEnrolled(student.getId(), course.getId())) {
            throw new BadRequestException("You are already enrolled in this course");
        }

        Payment existingPayment = paymentRepository
                .findFirstByStudentIdAndCourseIdAndPaymentStatusOrderByCreatedAtDesc(
                        student.getId(), course.getId(), PaymentStatus.PENDING
                )
                .orElse(null);
        if (existingPayment != null && existingPayment.getCheckoutUrl() != null
                && existingPayment.getKhaltiPidx() != null) {
            return toResponse(existingPayment);
        }

        String purchaseOrderId = "OCMS-" + UUID.randomUUID();
        Payment payment = Payment.builder()
                .student(student)
                .course(course)
                .amount(course.getPrice())
                .paymentMethod(PaymentMethod.KHALTI)
                .purchaseOrderId(purchaseOrderId)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        Long amountPaisa;
        try {
            amountPaisa = course.getPrice().movePointRight(2).longValueExact();
        } catch (ArithmeticException ex) {
            throw new BadRequestException("Course price must have no more than two decimal places");
        }
        if (amountPaisa < 1000) {
            throw new BadRequestException("Khalti requires a minimum payment of Rs.10");
        }
        KhaltiInitiateRequest payload = new KhaltiInitiateRequest(
                khaltiReturnUrl,
                khaltiWebsiteUrl,
                amountPaisa,
                purchaseOrderId,
                course.getTitle(),
                Map.of(
                        "name", student.getFirstName() + " " + student.getLastName(),
                        "email", student.getEmail(),
                        "phone", student.getPhoneNumber() == null ? "" : student.getPhoneNumber()
                )
        );

        try {
            RestClient restClient = RestClient.builder()
                    .baseUrl(khaltiApiBaseUrl)
                    .defaultHeader("Authorization", "Key " + khaltiSecretKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();
            KhaltiInitiateResponse gatewayResponse = restClient.post()
                    .uri("/epayment/initiate/")
                    .body(payload)
                    .retrieve()
                    .body(KhaltiInitiateResponse.class);
            if (gatewayResponse == null || gatewayResponse.pidx() == null
                    || gatewayResponse.payment_url() == null) {
                throw new BadRequestException("Khalti could not initiate the payment");
            }
            payment.setKhaltiPidx(gatewayResponse.pidx());
            payment.setCheckoutUrl(gatewayResponse.payment_url());
            payment.setGatewayResponse("pidx=" + gatewayResponse.pidx());
            return toResponse(paymentRepository.save(payment));
        } catch (RestClientResponseException ex) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse(ex.getResponseBodyAsString());
            paymentRepository.save(payment);
            throw new BadRequestException("Khalti payment initiation failed");
        } catch (RestClientException ex) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse("Khalti network/API error");
            paymentRepository.save(payment);
            throw new BadRequestException("Khalti payment initiation failed");
        }
    }

    @Transactional
    public PaymentResponse verifyByPidx(String pidx, String purchaseOrderId, String callbackStatus) {
        if (pidx == null || pidx.isBlank()) {
            throw new BadRequestException("Khalti payment identifier is required");
        }
        Payment payment = paymentRepository.findByKhaltiPidx(pidx)
                .orElseThrow(() -> new ResourceNotFoundException("Khalti payment not found"));
        User student = userService.getAuthenticatedUser();
        if (!payment.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("You cannot access another student's payment");
        }
        if (purchaseOrderId != null && !purchaseOrderId.isBlank()
                && !purchaseOrderId.equals(payment.getPurchaseOrderId())) {
            throw new BadRequestException("Khalti purchase order does not match this payment");
        }
        if ("User canceled".equalsIgnoreCase(callbackStatus)
                || "Canceled".equalsIgnoreCase(callbackStatus)
                || "Cancelled".equalsIgnoreCase(callbackStatus)) {
            payment.setPaymentStatus(PaymentStatus.CANCELLED);
            payment.setGatewayResponse("callback_status=" + callbackStatus);
            return toResponse(paymentRepository.save(payment));
        }
        if ("Expired".equalsIgnoreCase(callbackStatus)) {
            payment.setPaymentStatus(PaymentStatus.EXPIRED);
            payment.setGatewayResponse("callback_status=" + callbackStatus);
            return toResponse(paymentRepository.save(payment));
        }
        return verifyKhaltiPayment(payment.getId(), pidx);
    }

    @Transactional
    public PaymentResponse verifyKhaltiPayment(Long id, String callbackPidx) {
        User student = userService.getAuthenticatedUser();
        Payment payment = paymentRepository.findByIdAndStudentId(id, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getKhaltiPidx() == null) {
            throw new BadRequestException("This payment was not initiated with Khalti");
        }
        if (callbackPidx != null && !callbackPidx.equals(payment.getKhaltiPidx())) {
            throw new BadRequestException("Khalti payment identifier does not match this payment");
        }
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return toResponse(payment);
        }

        KhaltiLookupResponse lookup;
        try {
            RestClient restClient = RestClient.builder()
                    .baseUrl(khaltiApiBaseUrl)
                    .defaultHeader("Authorization", "Key " + khaltiSecretKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();
            lookup = restClient.post()
                    .uri("/epayment/lookup/")
                    .body(Map.of("pidx", payment.getKhaltiPidx()))
                    .retrieve()
                    .body(KhaltiLookupResponse.class);
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("Unable to verify Khalti transaction");
        } catch (RestClientException ex) {
            throw new BadRequestException("Unable to connect to Khalti for verification");
        }
        if (lookup == null || lookup.pidx() == null) {
            throw new BadRequestException("Invalid Khalti lookup response");
        }
        if (!payment.getKhaltiPidx().equals(lookup.pidx())) {
            throw new BadRequestException("Khalti lookup identifier mismatch");
        }

        Long expectedPaisa = payment.getAmount().movePointRight(2).longValueExact();
        if (!expectedPaisa.equals(lookup.total_amount())) {
            throw new BadRequestException("Khalti transaction amount mismatch");
        }

        payment.setGatewayResponse("pidx=" + lookup.pidx()
                + ";status=" + lookup.status()
                + ";transaction_id=" + lookup.transaction_id());
        String lookupStatus = lookup.status() == null ? "" : lookup.status();
        if ("Completed".equalsIgnoreCase(lookupStatus)
                && (lookup.refunded() == null || Boolean.FALSE.equals(lookup.refunded()))) {
            if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
                if (lookup.transaction_id() == null || lookup.transaction_id().isBlank()) {
                    throw new BadRequestException("Khalti completed transaction has no transaction ID");
                }
                paymentRepository.findByTransactionId(lookup.transaction_id())
                        .filter(existing -> !existing.getId().equals(payment.getId()))
                        .ifPresent(existing -> {
                            throw new BadRequestException("This Khalti transaction has already been processed");
                        });
                payment.setTransactionId(lookup.transaction_id());
                payment.setPaymentStatus(PaymentStatus.SUCCESS);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
                Course course = payment.getCourse();
                if (!enrollmentService.isEnrolled(student.getId(), course.getId())) {
                    enrollmentService.enrollPaidCourse(student, course);
                }
                notificationService.notify(
                        student,
                        "Payment successful",
                        "We received Rs." + payment.getAmount().stripTrailingZeros().toPlainString()
                                + " for \"" + course.getTitle() + "\".",
                        NotificationType.PAYMENT,
                        "/payment/success/" + payment.getId()
                );
            }
        } else if ("Pending".equalsIgnoreCase(lookupStatus)
                || "Initiated".equalsIgnoreCase(lookupStatus)) {
            payment.setPaymentStatus(PaymentStatus.PENDING);
        } else if ("Expired".equalsIgnoreCase(lookupStatus)) {
            payment.setPaymentStatus(PaymentStatus.EXPIRED);
        } else if ("Refunded".equalsIgnoreCase(lookupStatus)
                || Boolean.TRUE.equals(lookup.refunded())) {
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
        } else if ("Canceled".equalsIgnoreCase(lookupStatus)
                || "User canceled".equalsIgnoreCase(lookupStatus)
                || "Cancelled".equalsIgnoreCase(lookupStatus)) {
            payment.setPaymentStatus(PaymentStatus.CANCELLED);
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
        }
        return toResponse(paymentRepository.save(payment));
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
                .purchaseOrderId(payment.getPurchaseOrderId())
                .khaltiPidx(payment.getKhaltiPidx())
                .checkoutUrl(payment.getCheckoutUrl())
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
