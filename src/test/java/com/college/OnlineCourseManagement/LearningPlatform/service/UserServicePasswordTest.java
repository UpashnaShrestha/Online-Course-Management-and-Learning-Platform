package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ChangePasswordRequest;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.BadRequestException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.UserRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.AccountDeletionAuditRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.AssignmentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CertificateRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CourseRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.EnrollmentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.LessonProgressRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.NotificationRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.PasswordResetOtpRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.PaymentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.ReviewRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.SubmissionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServicePasswordTest {

    @Mock
    private UserRepository userRepository;

    /** Not exercised here -- only needed because UserService depends on it. */
    @Mock
    private ProfileImageStorage profileImageStorage;
    @Mock private AccountDeletionAuditRepository accountDeletionAuditRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private CertificateRepository certificateRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private PasswordResetOtpRepository passwordResetOtpRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private SubmissionRepository submissionRepository;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("student")
                .email("student@example.com")
                .role(Role.STUDENT)
                .password(passwordEncoder.encode("oldpass123"))
                .build();

        when(userRepository.findByUsername("student")).thenReturn(java.util.Optional.of(user));

        // changePassword resolves the caller from the SecurityContext.
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("student")
                .password(user.getPassword())
                .roles("STUDENT")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ChangePasswordRequest request(String current, String next, String confirm) {
        return ChangePasswordRequest.builder()
                .currentPassword(current)
                .newPassword(next)
                .confirmPassword(confirm)
                .build();
    }

    @Test
    @DisplayName("changePassword stores a BCrypt hash of the new password")
    void changePasswordStoresBcryptHash() {
        assertThatCode(() -> userService.changePassword(request("oldpass123", "newpass456", "newpass456")))
                .doesNotThrowAnyException();

        assertThat(user.getPassword()).isNotEqualTo("newpass456");
        assertThat(passwordEncoder.matches("newpass456", user.getPassword())).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("changePassword rejects a wrong current password")
    void wrongCurrentPasswordRejected() {
        assertThatThrownBy(() -> userService.changePassword(request("wrongpass", "newpass456", "newpass456")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("changePassword rejects mismatched confirmation")
    void mismatchedConfirmationRejected() {
        assertThatThrownBy(() -> userService.changePassword(request("oldpass123", "newpass456", "different99")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("do not match");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("changePassword refuses to reuse the current password")
    void samePasswordRejected() {
        assertThatThrownBy(() -> userService.changePassword(request("oldpass123", "oldpass123", "oldpass123")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("different");

        verify(userRepository, never()).save(any());
    }
}