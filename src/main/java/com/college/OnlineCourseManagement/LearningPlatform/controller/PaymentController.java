package com.college.OnlineCourseManagement.LearningPlatform.controller;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.PaymentCreateRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.PaymentResponse;
import com.college.OnlineCourseManagement.LearningPlatform.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/payments/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody PaymentCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Pending payment created successfully",
                        paymentService.createPendingPayment(request)
                ));
    }

    @GetMapping("/payments/khalti/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyKhalti(
            @RequestParam("pidx") String pidx,
            @RequestParam(value = "purchase_order_id", required = false) String purchaseOrderId,
            @RequestParam(value = "status", required = false) String callbackStatus
    ) {
        PaymentResponse payment = paymentService.verifyByPidx(pidx, purchaseOrderId, callbackStatus);
        return ResponseEntity.ok(ApiResponse.success("Khalti payment verified", payment));
    }

    @GetMapping("/student/payments")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getStudentPayments() {
        return ResponseEntity.ok(ApiResponse.success(
                "Payment history fetched successfully",
                paymentService.getStudentPayments()
        ));
    }

    @GetMapping("/student/payments/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getStudentPayment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Payment fetched successfully",
                paymentService.getStudentPayment(id)
        ));
    }

    @GetMapping("/admin/payments")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getAdminPayments() {
        return ResponseEntity.ok(ApiResponse.success(
                "Payments fetched successfully",
                paymentService.getAdminPayments()
        ));
    }

    @GetMapping("/admin/revenue")
    public ResponseEntity<ApiResponse<BigDecimal>> getAdminRevenue() {
        return ResponseEntity.ok(ApiResponse.success(
                "Revenue fetched successfully",
                paymentService.getAdminRevenue()
        ));
    }

    @GetMapping("/instructor/courses/{courseId}/revenue")
    public ResponseEntity<ApiResponse<BigDecimal>> getInstructorRevenue(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Course revenue fetched successfully",
                paymentService.getInstructorRevenue(courseId)
        ));
    }
}
