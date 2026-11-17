package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ForgotPasswordRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.ForgotPasswordResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.ResetPasswordRequest;
import com.college.OnlineCourseManagement.LearningPlatform.entity.PasswordResetOtp;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.BadRequestException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.PasswordResetOtpRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordResetServiceTest {

    private static final String INVALID = "That reset code is invalid or has expired. Please request a new one.";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetOtpRepository otpRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private OtpAttemptRecorder attemptRecorder;

    private PasswordResetService service;
    private PasswordEncoder passwordEncoder;
    private User user;
    private String salt;
    private String otp;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        OtpService realOtp = new OtpService(OtpService.defaultLength());
        service = new PasswordResetService(userRepository, otpRepository, passwordEncoder,
                notificationService, realOtp, emailService, attemptRecorder);

        ReflectionTestUtils.setField(service, "otpValidMinutes", 10);
        ReflectionTestUtils.setField(service, "maxAttempts", 5);
        ReflectionTestUtils.setField(service, "resendCooldownSeconds", 60L);
        ReflectionTestUtils.setField(service, "exposeOtp", false);

        user = User.builder()
                .id(1L)
                .username("student")
                .email("student@example.com")
                .firstName("Aarav")
                .role(Role.STUDENT)
                .password(passwordEncoder.encode("oldpass123"))
                .build();

        salt = realOtp.newSalt();
        otp = realOtp.generate();
    }

    private void stubOutstanding() {
        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(otpRecord(0, false)));
    }

    private PasswordResetOtp otpRecord(int attempts, boolean used) {
        return PasswordResetOtp.builder()
                .id(1L)
                .user(user)
                .salt(salt)
                .otpHash(new OtpService(OtpService.defaultLength()).hash(salt, otp))
                .attempts(attempts)
                .used(used)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ResetPasswordRequest request(String code) {
        return ResetPasswordRequest.builder()
                .email("student@example.com")
                .otp(code)
                .newPassword("brandnew1")
                .confirmPassword("brandnew1")
                .build();
    }

    @Test
    @DisplayName("requestReset emails a code and stores only a salted hash")
    void requestResetEmailsHashedCode() {
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(otpRepository.findByUserIdAndUsedFalse(1L)).thenReturn(List.of());
        when(otpRepository.save(any(PasswordResetOtp.class))).thenAnswer(i -> i.getArgument(0));

        service.requestReset(ForgotPasswordRequest.builder().email("student@example.com").build());

        ArgumentCaptor<PasswordResetOtp> captor = ArgumentCaptor.forClass(PasswordResetOtp.class);
        verify(otpRepository).save(captor.capture());

        PasswordResetOtp stored = captor.getValue();
        // Six digits, salted, and the plaintext code is nowhere in the record.
        assertThat(stored.getOtpHash()).hasSize(64);
        assertThat(stored.getSalt()).isNotBlank();
        assertThat(stored.getOtpHash()).isNotEqualTo(otp);

        verify(emailService).sendPasswordResetOtp(eq("student@example.com"), eq("Aarav"), anyString(), eq(10));
    }

    @Test
    @DisplayName("an unregistered address gets an identical, silent response")
    void unregisteredAddressIsSilent() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        ForgotPasswordResponse response = service.requestReset(
                ForgotPasswordRequest.builder().email("nobody@example.com").build());

        assertThat(response.getOtp()).isNull();
        verify(otpRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetOtp(anyString(), anyString(), anyString(), any(Integer.class));
    }

    @Test
    @DisplayName("a resend inside the cooldown is throttled")
    void resendIsThrottled() {
        PasswordResetOtp recent = otpRecord(0, false);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(recent));

        service.requestReset(ForgotPasswordRequest.builder().email("student@example.com").build());

        verify(otpRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetOtp(anyString(), anyString(), anyString(), any(Integer.class));
    }

    @Test
    @DisplayName("requesting a new code invalidates the previous one")
    void previousCodeInvalidated() {
        PasswordResetOtp previous = otpRecord(0, false);
        previous.setCreatedAt(LocalDateTime.now().minusMinutes(30));

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(previous));
        when(otpRepository.findByUserIdAndUsedFalse(1L)).thenReturn(List.of(previous));
        when(otpRepository.save(any(PasswordResetOtp.class))).thenAnswer(i -> i.getArgument(0));

        service.requestReset(ForgotPasswordRequest.builder().email("student@example.com").build());

        assertThat(previous.isUsed()).isTrue();
    }

    @Test
    @DisplayName("a correct code sets the new password and consumes the code")
    void correctCodeResetsPassword() {
        PasswordResetOtp record = otpRecord(0, false);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(record));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(otpRepository.save(any(PasswordResetOtp.class))).thenAnswer(i -> i.getArgument(0));

        assertThatCode(() -> service.resetPassword(request(otp))).doesNotThrowAnyException();

        assertThat(user.getPassword()).isNotEqualTo("brandnew1");
        assertThat(passwordEncoder.matches("brandnew1", user.getPassword())).isTrue();
        assertThat(record.isUsed()).isTrue();
        verify(emailService).sendSimple(eq("student@example.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("a wrong code is recorded via the separate transaction and never leaks the account")
    void wrongCodeCountsAttempt() {
        PasswordResetOtp record = otpRecord(0, false);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.resetPassword(request("000000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(INVALID);

        // Recorded in its own transaction, so the rejection cannot roll the counter back.
        verify(attemptRecorder).recordFailure(1L, 5);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("the attempt recorder is what enforces the brute-force limit")
    void attemptLimitDelegatesToRecorder() {
        PasswordResetOtp record = otpRecord(4, false);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.resetPassword(request("000000")))
                .isInstanceOf(BadRequestException.class);

        verify(attemptRecorder).recordFailure(1L, 5);
    }

    @Test
    @DisplayName("an expired code is rejected with the same generic message")
    void expiredCodeRejected() {
        PasswordResetOtp record = otpRecord(0, false);
        record.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.resetPassword(request(otp)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(INVALID);
    }

    @Test
    @DisplayName("an unregistered email is indistinguishable from a wrong code")
    void unregisteredEmailGivesSameMessage() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(ResetPasswordRequest.builder()
                .email("nobody@example.com").otp("123456")
                .newPassword("brandnew1").confirmPassword("brandnew1").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(INVALID);
    }

    @Test
    @DisplayName("no outstanding code is rejected cleanly, not a 500")
    void noOutstandingCodeRejected() {
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(request(otp)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(INVALID);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("an already-consumed code cannot be replayed")
    void consumedCodeRejected() {
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(request(otp)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(INVALID);
    }

    @Test
    @DisplayName("mismatched confirmation is rejected before touching any code")
    void mismatchedConfirmationRejected() {
        assertThatThrownBy(() -> service.resetPassword(ResetPasswordRequest.builder()
                .email("student@example.com").otp("123456")
                .newPassword("brandnew1").confirmPassword("different9").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("do not match");

        verify(otpRepository, never()).findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(any());
    }
}