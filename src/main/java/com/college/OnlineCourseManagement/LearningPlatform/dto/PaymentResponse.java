package com.college.OnlineCourseManagement.LearningPlatform.dto;

import com.college.OnlineCourseManagement.LearningPlatform.enums.PaymentMethod;
import com.college.OnlineCourseManagement.LearningPlatform.enums.PaymentStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class PaymentResponse {
    Long id;
    String transactionId;
    String purchaseOrderId;
    String khaltiPidx;
    String checkoutUrl;
    Long courseId;
    String courseTitle;
    BigDecimal amount;
    PaymentMethod paymentMethod;
    PaymentStatus paymentStatus;
    LocalDateTime paidAt;
    LocalDateTime createdAt;
}
