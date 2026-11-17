package com.college.OnlineCourseManagement.LearningPlatform.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends transactional email over SMTP.
 *
 * Every method is fire-and-forget: a mail outage must never roll back the database work
 * that triggered the message, so failures are logged rather than propagated.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    /** Front-end origin used to build the links inside emails. */
    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.mail.dev-log-otp:true}")
    private boolean devLogOtp;

    /**
     * Which .env file supplied the configuration, for the startup banner. Set by
     * DotEnvPropertySourcePostProcessor; "(none)" when only OS variables were used.
     */
    @Value("${app.dotenv.location:(none)}")
    private String dotEnvLocation;

    public boolean isMailEnabled() {
        // Username is required too: without it every send would fail authentication, so
        // treat an incomplete configuration as "off" and say so once at startup.
        return mailEnabled
                && mailHost != null && !mailHost.isBlank()
                && fromAddress != null && !fromAddress.isBlank();
    }

    private String sender() {
        return fromAddress == null || fromAddress.isBlank() ? "no-reply@courseflow.local" : fromAddress;
    }

    @Async
    public void sendSimple(String to, String subject, String body) {
        if (!isMailEnabled()) {
            log.warn("Mail is disabled, skipping email to {} ({})", to, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sent '{}' to {}", subject, to);
        } catch (MailException ex) {
            log.error("Failed to send '{}' to {}", subject, to, ex);
        }
    }

    /** Password reset email carrying the one-time code. */
    @Async
    public void sendPasswordResetOtp(String to, String firstName, String otp, int validMinutes) {
        // Says "code" rather than "link": the flow is a typed OTP, and an email promising a
        // link leaves the reader looking for a button that is not there.
        String subject = "Your CourseFlow password reset code";

        if (!isMailEnabled()) {
            log.warn("""
                    Password reset email NOT sent to {} - email is not configured.
                      spring.mail.username = {}
                      .env used            = {}""",
                    to,
                    fromAddress == null || fromAddress.isBlank() ? "(BLANK)" : fromAddress,
                    dotEnvLocation);
            // Without a mail server there is no way to deliver the code, so surface it in
            // the logs rather than silently stranding the user.
            if (devLogOtp) {
                log.warn("=== DEV ONLY: reset OTP for {} is {} (valid {} min) ===", to, otp, validMinutes);
            }
            return;
        }

        String body = """
                Hi %s,

                We received a request to reset the password for your CourseFlow account.

                Your one-time code is:  %s

                Enter this code on the reset page to choose a new password.
                It expires in %d minutes and can only be used once.

                If you did not request this, you can safely ignore this email -
                your password will not change.

                -- CourseFlow
                """.formatted(firstName, otp, validMinutes);

        sendSimple(to, subject, body);
    }

    @Async
    public void sendWelcome(String to, String firstName) {
        sendSimple(to, "Welcome to CourseFlow", """
                Hi %s,

                Your CourseFlow account is ready. Browse the catalog and enroll in your first
                course to get started.

                %s

                -- CourseFlow
                """.formatted(firstName, frontendBaseUrl));
    }

    /**
     * Logs the effective configuration once at startup.
     *
     * This exists because the failure is otherwise invisible: with a blank SMTP username the
     * reset endpoint still answers "if an account exists, a code has been sent", so nothing
     * reaches the user and nothing in the UI explains why. The message names the .env file
     * that was actually read, since a wrong working directory is the usual cause.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logConfiguration() {
        log.info("Configuration read from .env: {}", dotEnvLocation);

        if (!mailEnabled) {
            log.info("Email is disabled (app.mail.enabled=false). Password reset codes will only be logged.");
            return;
        }

        if (!isMailEnabled()) {
            log.warn("""
                    Email is ENABLED but INCOMPLETELY CONFIGURED - no mail will be sent, and \
                    password resets will not work.
                      spring.mail.host     = {}
                      spring.mail.username = {}
                      .env used            = {}
                    If that file should have supplied the credentials, the app is probably \
                    running from the wrong working directory. Set MAIL_USERNAME and \
                    MAIL_PASSWORD in the environment to override.""",
                    mask(mailHost),
                    fromAddress == null || fromAddress.isBlank() ? "(BLANK - this is the problem)" : fromAddress,
                    dotEnvLocation);
            return;
        }

        log.info("Email enabled via {} as {}", mailHost, fromAddress);
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return "(blank)";
        }
        return value.length() <= 4 ? "***" : value.substring(0, 2) + "***";
    }
}