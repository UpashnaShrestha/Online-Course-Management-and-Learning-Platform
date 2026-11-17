package com.college.OnlineCourseManagement.LearningPlatform.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.stream.Collectors;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            // Let @NotBlank own the "required" case so the user is not told about
            // composition rules for a field they simply have not filled in.
            return true;
        }

        List<PasswordPolicy.Rule> failures = PasswordPolicy.failures(password);
        if (failures.isEmpty()) {
            return true;
        }

        String detail = failures.stream()
                .map(PasswordPolicy.Rule::requirement)
                .collect(Collectors.joining(", "));

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("Password needs: " + detail)
                .addConstraintViolation();

        return false;
    }
}