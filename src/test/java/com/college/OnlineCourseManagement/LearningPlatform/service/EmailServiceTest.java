package com.college.OnlineCourseManagement.LearningPlatform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService service;

    @BeforeEach
    void setUp() {
        service = new EmailService(mailSender);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@courseflow.test");
        ReflectionTestUtils.setField(service, "mailHost", "smtp.gmail.com");
        ReflectionTestUtils.setField(service, "mailEnabled", true);
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(service, "devLogOtp", true);
    }

    private SimpleMailMessage capture() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("the reset email contains the code, the recipient and a sensible from address")
    void resetOtpEmailContent() {
        service.sendPasswordResetOtp("learner@example.com", "Aarav", "123456", 10);

        SimpleMailMessage message = capture();

        assertThat(message.getTo()).containsExactly("learner@example.com");
        assertThat(message.getFrom()).isEqualTo("noreply@courseflow.test");
        assertThat(message.getSubject()).contains("reset code");
        assertThat(message.getText())
                .contains("Aarav")
                .contains("123456")
                .contains("10 minutes")
                .contains("only be used once");
    }

    @Test
    @DisplayName("the email explains that ignoring it changes nothing")
    void resetOtpEmailReassuresUser() {
        service.sendPasswordResetOtp("learner@example.com", "Aarav", "123456", 10);

        assertThat(capture().getText()).contains("did not request this");
    }

    @Test
    @DisplayName("a mail failure is swallowed so the caller keeps working")
    void mailFailureIsSwallowed() {
        // JavaMailSender#send returns void, so it must be stubbed with doThrow.
        doThrow(new MailAuthenticationException("bad credentials"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> service.sendPasswordResetOtp("learner@example.com", "A", "123456", 10))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("nothing is sent when mail is switched off")
    void nothingSentWhenDisabled() {
        ReflectionTestUtils.setField(service, "mailEnabled", false);

        service.sendPasswordResetOtp("learner@example.com", "A", "123456", 10);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("a blank username counts as not configured")
    void blankUsernameCountsAsDisabled() {
        ReflectionTestUtils.setField(service, "fromAddress", "");

        assertThat(service.isMailEnabled()).isFalse();

        service.sendPasswordResetOtp("learner@example.com", "A", "123456", 10);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("the password-changed notice is sent after a successful reset")
    void passwordChangedNotice() {
        service.sendSimple("learner@example.com", "Your CourseFlow password was changed", "body");

        SimpleMailMessage message = capture();
        assertThat(message.getSubject()).contains("password was changed");
    }
}