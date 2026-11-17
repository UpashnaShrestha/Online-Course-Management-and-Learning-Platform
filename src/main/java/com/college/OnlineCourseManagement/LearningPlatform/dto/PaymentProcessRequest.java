package com.college.OnlineCourseManagement.LearningPlatform.dto;

import com.college.OnlineCourseManagement.LearningPlatform.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessRequest {

    @NotNull(message = "Payment outcome is required")
    private Boolean successful;

    /** Optional: the dummy payment method picked by the student on the checkout page. */
    private PaymentMethod paymentMethod;
}
