package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ForgotPasswordRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.ForgotPasswordResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.ResetPasswordRequest;
import com.college.OnlineCourseManagement.LearningPlatform.entity.PasswordResetOtp;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.NotificationType;
import com.college.OnlineCourseManagement.LearningPlatform.exception.BadRequestException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.PasswordResetOtpRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final OtpAttemptRecorder attemptRecorder;

    @Value("${app.password-reset.otp-valid-minutes:10}")
    private int otpValidMinutes;

    @Value("${app.password-reset.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.password-reset.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Value("${app.password-reset.expose-otp:false}")
    private boolean exposeOtp;


    public ForgotPasswordResponse requestReset(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            log.info("Password reset requested for an unregistered address");
            return ForgotPasswordResponse.builder().build();
        }

        PasswordResetOtp existing = otpRepository
                .findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElse(null);

        if (existing != null && !existing.isExpired()
                && existing.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(resendCooldownSeconds))) {
            log.info("Reset code already sent recently for user id {}; ignoring resend", user.getId());
            return ForgotPasswordResponse.builder().build();
        }


        List<PasswordResetOtp> outstanding = otpRepository.findByUserIdAndUsedFalse(user.getId());
        outstanding.forEach(otp -> otp.setUsed(true));
        otpRepository.saveAll(outstanding);

        String otp = otpService.generate();
        String salt = otpService.newSalt();

        otpRepository.save(PasswordResetOtp.builder()
                .salt(salt)
                .otpHash(otpService.hash(salt, otp))
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(otpValidMinutes))
                .attempts(0)
                .used(false)
                .build());

        String firstName = user.getFirstName() == null ? "there" : user.getFirstName();
        emailService.sendPasswordResetOtp(user.getEmail(), firstName, otp, otpValidMinutes);

        if (exposeOtp) {
            log.warn("app.password-reset.expose-otp is ENABLED - returning the OTP in the API response");
            return ForgotPasswordResponse.builder().otp(otp).build();
        }

        return ForgotPasswordResponse.builder().build();
    }


    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }

        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            throw new BadRequestException(INVALID_CODE_MESSAGE);
        }

        PasswordResetOtp record = otpRepository
                .findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElse(null);

        if (record == null || !record.isUsable(maxAttempts)) {
            // No outstanding, unexpired code: throw the same message as a wrong code.
            throw new BadRequestException(INVALID_CODE_MESSAGE);
        }

        if (!otpService.matches(record, request.getOtp().trim())) {

            attemptRecorder.recordFailure(record.getId(), maxAttempts);

            log.info("Incorrect OTP for user id {} (attempt recorded)", user.getId());

            throw new BadRequestException(INVALID_CODE_MESSAGE);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        record.setUsed(true);
        otpRepository.save(record);

        notificationService.notify(
                user,
                "Password changed",
                "Your password was reset with a one-time code. If this wasn't you, contact support immediately.",
                NotificationType.SYSTEM,
                "/login"
        );


        emailService.sendSimple(
                user.getEmail(),
                "Your CourseFlow password was changed",
                "Your password was just reset. If this was not you, contact support immediately."
        );
    }

    private static final String INVALID_CODE_MESSAGE =
            "That reset code is invalid or has expired. Please request a new one.";
}