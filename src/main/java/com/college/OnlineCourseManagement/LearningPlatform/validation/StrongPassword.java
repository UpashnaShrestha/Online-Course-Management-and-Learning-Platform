package com.college.OnlineCourseManagement.LearningPlatform.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applies {@link PasswordPolicy} to a password field.
 *
 * Replaces the loose {@code @Size(min = 6)} it used to sit alongside, which accepted
 * "abcdefgh". Use it instead of @Size so the rule is stated once.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Password does not meet the requirements";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}