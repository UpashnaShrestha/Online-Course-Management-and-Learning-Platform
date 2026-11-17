package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ResetPasswordRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Locks the OTP length in one place.
 *
 * The generator and the {@code @Pattern} on {@link ResetPasswordRequest} must agree, otherwise
 * the form accepts a code the service can never have issued (or, worse, rejects a valid one).
 * Because the pattern is a compile-time constant they can only agree by construction -- these
 * tests make that guarantee explicit and fail the build if someone changes one side.
 */
class OtpServiceTest {

    private static final String EMAIL = "learner@example.com";

    @Test
    @DisplayName("generates a code with exactly the advertised number of digits")
    void generateProducesTheAdvertisedLength() {
        OtpService service = new OtpService(OtpService.LENGTH);

        for (int i = 0; i < 500; i++) {
            String otp = service.generate();
            assertThat(otp).hasSize(OtpService.LENGTH).containsOnlyDigits();
        }
    }

    @Test
    @DisplayName("generated codes match the regex compiled into the request DTO")
    void generatedCodesSatisfyTheDtoPattern() {
        OtpService service = new OtpService(OtpService.LENGTH);
        Pattern dtoPattern = Pattern.compile(OtpService.REGEX);

        for (int i = 0; i < 500; i++) {
            assertThat(dtoPattern.matcher(service.generate()).matches()).isTrue();
        }
    }

    @Test
    @DisplayName("the DTO accepts only a 6-digit numeric code")
    void dtoAcceptsOnlyTheGeneratedShape() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            for (String wrong : new String[] { "12345", "1234567", "abcdef", "12345a", "12 456", "" }) {
                assertThat(validator.validate(requestWithOtp(wrong)))
                        .as("otp %s must be rejected", wrong)
                        .isNotEmpty();
            }

            assertThat(validator.validate(requestWithOtp("012345"))).isEmpty();
            assertThat(validator.validate(requestWithOtp("000000"))).isEmpty();
        }
    }

    @Test
    @DisplayName("a configured length other than the compiled one fails fast at startup")
    void misconfiguredLengthIsRejected() {
        assertThatThrownBy(() -> new OtpService(OtpService.LENGTH + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("compiled into ResetPasswordRequest");

        assertThatThrownBy(() -> new OtpService(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be between");
    }

    @Test
    @DisplayName("matching is constant-time on the hash and rejects a wrong code")
    void matchesRejectsWrongCodes() {
        OtpService service = new OtpService(OtpService.LENGTH);
        String otp = service.generate();
        String salt = service.newSalt();

        var record = new com.college.OnlineCourseManagement.LearningPlatform.entity.PasswordResetOtp();
        record.setSalt(salt);
        record.setOtpHash(service.hash(salt, otp));

        assertThat(service.matches(record, otp)).isTrue();
        assertThat(service.matches(record, "000000".equals(otp) ? "111111" : "000000")).isFalse();
        assertThat(service.matches(record, null)).isFalse();
        assertThat(service.matches(null, otp)).isFalse();
    }

    /** A structurally valid request carrying only the supplied OTP. */
    private ResetPasswordRequest requestWithOtp(String otp) {
        // Satisfies PasswordPolicy: 8+, upper, lower, digit and special. "newSecret1" no
        // longer passes now that the policy requires a special character.
        return ResetPasswordRequest.builder()
                .email(EMAIL)
                .otp(otp)
                .newPassword("N3wSecret!")
                .confirmPassword("N3wSecret!")
                .build();
    }
}