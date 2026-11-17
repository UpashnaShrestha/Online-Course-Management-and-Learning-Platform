package com.college.OnlineCourseManagement.LearningPlatform.enums;

import com.college.OnlineCourseManagement.LearningPlatform.dto.CourseRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Level and duration used to be free text, which let "beginner" and "BEGINNER" coexist and
 * showed up as duplicate entries in the catalog filter. These tests pin the closed sets.
 */
class CourseLevelTest {

    @ParameterizedTest
    @ValueSource(strings = { "Beginner", "beginner", "BEGINNER", "  Beginner  ", "beginner-level" })
    @DisplayName("fromText is lenient about case, spacing and separators")
    void lenientLookup(String input) {
        assertThat(CourseLevel.fromText(input)).isEqualTo(CourseLevel.BEGINNER);
    }

    @ParameterizedTest
    @ValueSource(strings = { "wizard", "expert", "1", "beginners" })
    @DisplayName("fromText rejects values outside the closed set")
    void rejectsUnknown(String input) {
        assertThat(CourseLevel.fromText(input)).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "   " })
    @DisplayName("blank input yields null rather than throwing")
    void blankIsNull(String input) {
        assertThat(CourseLevel.fromText(input)).isNull();
    }

    @Test
    @DisplayName("exactly three levels exist, each with a display label")
    void threeLevels() {
        assertThat(CourseLevel.values()).hasSize(3);
        for (CourseLevel level : CourseLevel.values()) {
            assertThat(level.getLabel()).isNotBlank();
            // The label is what gets stored, so it must round-trip.
            assertThat(CourseLevel.fromText(level.getLabel())).isEqualTo(level);
        }
    }
}

class CourseDurationTest {

    @ParameterizedTest
    @CsvSource({
            "Under 1 hour, UNDER_1_HOUR",
            "1-3 hours, HOURS_1_3",
            "3-6 hours, HOURS_3_6",
            "6-10 hours, HOURS_6_10",
            "10-20 hours, HOURS_10_20",
            "20-40 hours, HOURS_20_40",
            "1-4 weeks, WEEKS_1_4",
            "4-8 weeks, WEEKS_4_8",
            "8-12 weeks, WEEKS_8_12",
            "3-6 months, MONTHS_3_6",
            "6+ months, MONTHS_6_PLUS",
    })
    @DisplayName("every offered label resolves to its constant")
    void labelsResolve(String label, String constant) {
        assertThat(CourseDuration.fromText(label)).isEqualTo(CourseDuration.valueOf(constant));
    }

    @Test
    @DisplayName("approximate hours increase with the length of the course")
    void approximateHoursAreOrdered() {
        List<Integer> hours = Arrays.stream(CourseDuration.values())
                .map(CourseDuration::getApproximateHours)
                .toList();

        assertThat(hours).isSorted();
        assertThat(hours).doesNotContain(0);
    }

    @Test
    @DisplayName("options() is ordered shortest first and matches values()")
    void optionsAreOrdered() {
        assertThat(CourseDuration.options()).containsExactly(CourseDuration.values());
    }

    @ParameterizedTest
    @ValueSource(strings = { "6 weeks", "one hour", "a couple of days", "self-paced" })
    @DisplayName("a duration outside the offered set returns null so it can be reported")
    void unknownReturnsNull(String input) {
        assertThat(CourseDuration.fromText(input)).isNull();
        assertThat(CourseDuration.matches(input)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { "4-8 weeks", "4-8 Weeks", "  4-8 weeks  ", "4 to 8 weeks" })
    @DisplayName("an offered duration is matched regardless of case, spacing or wording")
    void toleratesSpellingVariants(String input) {
        assertThat(CourseDuration.fromText(input)).isEqualTo(CourseDuration.WEEKS_4_8);
    }
}

/** The dropdown the UI renders must be exactly what the API accepts. */
class CourseOptionValidationTest {

    private static Validator validator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator();
        }
    }

    private static CourseRequest withLevelAndDuration(String level, String duration) {
        return CourseRequest.builder()
                .title("A course")
                .price(new java.math.BigDecimal("250"))
                .level(level)
                .duration(duration)
                .categoryId(1L)
                .build();
    }

    @Test
    @DisplayName("every offered level and duration is accepted")
    void acceptsAllOfferedOptions() {
        for (CourseLevel level : CourseLevel.values()) {
            for (CourseDuration duration : CourseDuration.values()) {
                var violations = validator().validate(withLevelAndDuration(level.getLabel(), duration.getLabel()));
                assertThat(violations)
                        .as("level=%s duration=%s should be accepted", level.getLabel(), duration.getLabel())
                        .isEmpty();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "Expert", "wizard", "easy", "1" })
    @DisplayName("a level outside the closed set is rejected, listing what is allowed")
    void rejectsUnknownLevel(String level) {
        var violations = validator().validate(withLevelAndDuration(level, "4-8 weeks"));

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anySatisfy(v -> assertThat(v.getMessage()).contains("Beginner, Intermediate, Advanced"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "beginner", "BEGINNER", "beginner-level" })
    @DisplayName("a decorated or mis-cased level is accepted; CourseService canonicalises it")
    void acceptsAndCanonicalisesKnownLevels(String level) {
        // Validation accepts it because it resolves to a real level, and CourseService then
        // stores the canonical label. Rejecting it here would break existing clients.
        assertThat(validator().validate(withLevelAndDuration(level, "4-8 weeks"))).isEmpty();
        assertThat(CourseLevel.fromText(level).getLabel()).isEqualTo("Beginner");
    }

    @ParameterizedTest
    @ValueSource(strings = { "6 weeks", "One hour", "4-8 weeks please", "self-paced" })
    @DisplayName("a duration outside the closed set is rejected")
    void rejectsUnknownDuration(String duration) {
        assertThat(validator().validate(withLevelAndDuration("Beginner", duration))).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "4-8 weeks", "4-8 Weeks", "4 to 8 weeks" })
    @DisplayName("an offered duration is accepted whatever the spelling")
    void acceptsDurationSpellings(String duration) {
        assertThat(validator().validate(withLevelAndDuration("Beginner", duration))).isEmpty();
    }

    @Test
    @DisplayName("blank level and duration are allowed, so they stay optional")
    void blankIsAllowed() {
        assertThat(validator().validate(withLevelAndDuration(null, null))).isEmpty();
        assertThat(validator().validate(withLevelAndDuration("", ""))).isEmpty();
    }
}