package com.college.OnlineCourseManagement.LearningPlatform.validation;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ChangePasswordRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.RegisterRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.ResetPasswordRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The password policy is the kind of rule that is easy to weaken by accident: someone drops
 * a rule, or a test password that used to be valid stops being. These tests pin both the
 * accept and the reject sides.
 */
class PasswordPolicyTest {

    private static Validator validator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator();
        }
    }

    /** All five rules satisfied. */
    private static final String GOOD = "Str0ng!Pass";

    @ParameterizedTest
    @ValueSource(strings = {
            "Str0ng!Pass",
            "aB3$x!yz",   // was "aB3$xyz" -- missing a special character
            "P@ssw0rd!",
            "Aa1!aaaa",
            "LongEnough1!",
            "##Hello9",
    })
    @DisplayName("accepts passwords that satisfy every rule")
    void acceptsStrongPasswords(String password) {
        assertThat(PasswordPolicy.isValid(password))
                .as("password %s should be accepted", password)
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Sh0rt!",        // 7 chars
            "password",      // no upper, no digit, no special
            "password123",   // no upper, no special  <- the old seeded test password
            "PASSWORD123",   // no lower, no special
            "NoDigitsHere!", // no digit
            "nouppercase1!", // no uppercase
            "NoSpecial123",  // no special character
            "alllowercase1", // no upper, no special
            "12345678",      // digits only
            "!!!!!!!!",      // specials only
    })
    @DisplayName("rejects passwords that break a rule, and says which one")
    void rejectsWeakPasswords(String password) {
        assertThat(PasswordPolicy.isValid(password))
                .as("password %s should be rejected", password)
                .isFalse();
        assertThat(PasswordPolicy.failures(password))
                .as("password %s should report at least one unmet rule", password)
                .isNotEmpty();
    }

    @Test
    @DisplayName("null and empty are reported as missing, not as composition failures")
    void emptyFailsEveryRule() {
        assertThat(PasswordPolicy.failures("")).hasSize(PasswordPolicy.RULES.size());
        assertThat(PasswordPolicy.failures(null)).hasSize(PasswordPolicy.RULES.size());
    }

    @Test
    @DisplayName("longer than the maximum is rejected")
    void overlongPasswordRejected() {
        assertThat(PasswordPolicy.isValid("Aa1!" + "x".repeat(200))).isFalse();
    }

    @Test
    @DisplayName("the rule list is the five documented requirements")
    void rulesAreTheDocumentedFive() {
        List<String> keys = new ArrayList<>();
        for (PasswordPolicy.Rule rule : PasswordPolicy.RULES) {
            keys.add(rule.key());
        }
        assertThat(keys).containsExactly("minLength", "uppercase", "lowercase", "digit", "special");
    }

    // ---- the annotation is actually wired into every DTO that accepts a password ----

    @Test
    @DisplayName("register rejects a weak password with a helpful message")
    void registerRejectsWeakPassword() {
        var violations = validator().validate(registerWith("password123"));

        assertThat(violations).isNotEmpty();
        assertThat(messages(violations))
                .anySatisfy(m -> assertThat(m).contains("Password needs:"));
    }

    @Test
    @DisplayName("register accepts a strong password")
    void registerAcceptsStrongPassword() {
        assertThat(validator().validate(registerWith(GOOD))).isEmpty();
    }

    @Test
    @DisplayName("change-password enforces the same policy")
    void changePasswordEnforcesPolicy() {
        var request = ChangePasswordRequest.builder()
                .currentPassword("whatever")
                .newPassword("password123")
                .confirmPassword("password123")
                .build();

        assertThat(validator().validate(request)).isNotEmpty();

        request.setNewPassword(GOOD);
        request.setConfirmPassword(GOOD);
        assertThat(validator().validate(request)).isEmpty();
    }

    @Test
    @DisplayName("reset-password enforces the same policy")
    void resetPasswordEnforcesPolicy() {
        var weak = ResetPasswordRequest.builder()
                .email("learner@example.com")
                .otp("123456")
                .newPassword("password123")
                .confirmPassword("password123")
                .build();
        assertThat(validator().validate(weak)).isNotEmpty();

        var strong = ResetPasswordRequest.builder()
                .email("learner@example.com")
                .otp("123456")
                .newPassword(GOOD)
                .confirmPassword(GOOD)
                .build();
        assertThat(validator().validate(strong)).isEmpty();
    }

    @Test
    @DisplayName("a blank password is reported as required, not as five unmet rules")
    void blankPasswordReportsRequired() {
        var messages = messages(validator().validate(registerWith("")));

        assertThat(messages).contains("Password is required");
        assertThat(messages).noneMatch(m -> m.contains("Password needs:"));
    }

    private static RegisterRequest registerWith(String password) {
        return RegisterRequest.builder()
                .username("aarav")
                .firstName("Aarav")
                .lastName("Shrestha")
                .email("aarav@example.com")
                .password(password)
                .build();
    }

    private static Set<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }
}