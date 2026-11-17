package com.college.OnlineCourseManagement.LearningPlatform.validation;

import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseDuration;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseLevel;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Restricts a field to one of a closed set of values, e.g. {@code @OneOf(CourseLevel.class)}.
 *
 * The UI offers a dropdown, but a dropdown is only a convenience: an API client can still post
 * any string. This rejects the unknown value instead of storing free text that would later
 * appear as a stray entry in the catalog's filter dropdown.
 */
@Documented
@Constraint(validatedBy = OneOf.OneOfValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface OneOf {

    String message() default "Value is not one of the allowed options";

    /** The enum whose constants are accepted. */
    Class<? extends Enum<?>> value();

    /**
     * Whether to match via the enum's own lenient parser instead of comparing display labels.
     *
     * Set this when the enum exposes a {@code fromText(String)} method, so "4 to 8 weeks" and
     * "4-8 Weeks" are accepted rather than rejected over spacing and case. What gets stored
     * is still the caller's input; use {@link Canonicalise} to normalise it on the way in.
     */
    boolean lenient() default true;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class OneOfValidator implements ConstraintValidator<OneOf, String> {

        private List<String> allowed;
        private String allowedList;
        private java.lang.reflect.Method fromText;
        private boolean lenient;

        @Override
        public void initialize(OneOf annotation) {
            Class<? extends Enum<?>> type = annotation.value();
            lenient = annotation.lenient();

            allowed = java.util.Arrays.stream(type.getEnumConstants())
                    .map(constant -> {
                        // CourseLevel/CourseDuration carry a display label; prefer it so the
                        // stored value matches what the dropdown offered.
                        try {
                            return (String) constant.getClass().getMethod("getLabel").invoke(constant);
                        } catch (ReflectiveOperationException ex) {
                            return constant.name();
                        }
                    })
                    .toList();
            allowedList = String.join(", ", allowed);

            if (lenient) {
                try {
                    fromText = type.getMethod("fromText", String.class);
                } catch (NoSuchMethodException ex) {
                    // No parser available: fall back to exact label matching.
                    fromText = null;
                }
            }
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.isBlank()) {
                return true;
            }

            if (matches(value)) {
                return true;
            }

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Must be one of: " + allowedList).addConstraintViolation();
            return false;
        }

        private boolean matches(String value) {
            String trimmed = value.trim();

            if (allowed.contains(trimmed)) {
                return true;
            }

            if (fromText == null) {
                return false;
            }

            try {
                return fromText.invoke(null, trimmed) != null;
            } catch (ReflectiveOperationException | IllegalArgumentException ex) {
                return false;
            }
        }
    }
}