package com.college.OnlineCourseManagement.LearningPlatform.repository;

import com.college.OnlineCourseManagement.LearningPlatform.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    boolean existsByUserId(Long userId);


    /** Only the most recent outstanding OTP for a user is honoured. */
    Optional<PasswordResetOtp> findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);

    List<PasswordResetOtp> findByUserIdAndUsedFalse(Long userId);
}