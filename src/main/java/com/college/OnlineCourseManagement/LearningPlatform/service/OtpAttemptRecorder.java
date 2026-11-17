package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.entity.PasswordResetOtp;
import com.college.OnlineCourseManagement.LearningPlatform.repository.PasswordResetOtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records failed OTP verification attempts.
 *
 * Lives in its own component on purpose: the caller rejects a bad code by throwing, which
 * rolls back the caller's transaction. Counting the attempt inside that transaction would
 * therefore be rolled back too, leaving the brute-force limit permanently at zero. A
 * REQUIRES_NEW transaction suspends the outer one and commits independently, so the counter
 * survives the rejection.
 */
@Component
@RequiredArgsConstructor
public class OtpAttemptRecorder {

    private final PasswordResetOtpRepository otpRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long otpId, int maxAttempts) {
        PasswordResetOtp record = otpRepository.findById(otpId).orElse(null);

        if (record == null || record.isUsed()) {
            return;
        }

        record.setAttempts(record.getAttempts() + 1);

        if (record.getAttempts() >= maxAttempts) {
            record.setUsed(true);
        }

        otpRepository.save(record);
    }
}