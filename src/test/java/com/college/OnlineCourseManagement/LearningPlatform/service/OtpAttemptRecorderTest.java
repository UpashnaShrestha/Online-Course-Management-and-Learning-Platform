package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.entity.PasswordResetOtp;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.repository.PasswordResetOtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtpAttemptRecorderTest {

    @Mock
    private PasswordResetOtpRepository otpRepository;

    private OtpAttemptRecorder recorder;

    @BeforeEach
    void setUp() {
        // Built here, not in a field initialiser: Mockito injects @Mock fields after
        // construction, so an initialiser would capture null.
        recorder = new OtpAttemptRecorder(otpRepository);
    }

    private PasswordResetOtp record(int attempts, boolean used) {
        return PasswordResetOtp.builder()
                .id(1L)
                .user(User.builder().id(1L).build())
                .salt("salt")
                .otpHash("hash")
                .attempts(attempts)
                .used(used)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
    }

    @Test
    @DisplayName("increments the attempt counter without consuming the code")
    void incrementsAttempts() {
        PasswordResetOtp otp = record(0, false);
        when(otpRepository.findById(1L)).thenReturn(Optional.of(otp));

        recorder.recordFailure(1L, 5);

        assertThat(otp.getAttempts()).isEqualTo(1);
        assertThat(otp.isUsed()).isFalse();
        verify(otpRepository).save(otp);
    }

    @Test
    @DisplayName("consumes the code once the limit is reached")
    void consumesAtLimit() {
        PasswordResetOtp otp = record(4, false);
        when(otpRepository.findById(1L)).thenReturn(Optional.of(otp));

        recorder.recordFailure(1L, 5);

        assertThat(otp.getAttempts()).isEqualTo(5);
        assertThat(otp.isUsed()).isTrue();
    }

    @Test
    @DisplayName("an already-consumed code is left alone")
    void consumedCodeUntouched() {
        PasswordResetOtp otp = record(5, true);
        when(otpRepository.findById(1L)).thenReturn(Optional.of(otp));

        recorder.recordFailure(1L, 5);

        assertThat(otp.getAttempts()).isEqualTo(5);
        verify(otpRepository, never()).save(any());
    }

    @Test
    @DisplayName("a missing record is a no-op")
    void missingRecordIsNoOp() {
        when(otpRepository.findById(99L)).thenReturn(Optional.empty());

        recorder.recordFailure(99L, 5);

        verify(otpRepository, never()).save(any());
    }
}