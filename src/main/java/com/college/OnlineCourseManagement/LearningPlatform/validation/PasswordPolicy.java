package com.college.OnlineCourseManagement.LearningPlatform.validation;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The single definition of what a valid password is.
 *
 * Register, change-password and reset-password all accept passwords, so the rules live here
 * once instead of being restated in each DTO. {@link StrongPassword} is the annotation that
 * applies them.
 *
 * Only new/changed passwords are checked. Existing stored passwords are never re-validated,
 * so tightening this policy does not lock anyone out.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 100;

    private static final Pattern UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL = Pattern.compile(".*[^A-Za-z0-9].*");

    /**
     * One entry per rule, in the order they are reported to the user.
     *
     * @param key        stable short identifier; mirrored by PASSWORD_RULES in the frontend
     * @param pattern    the character class to match, or null for the length rule
     * @param requirement human-readable requirement, shown in error messages and the UI checklist
     */
    public record Rule(String key, Pattern pattern, String requirement) {
    }

    public static final List<Rule> RULES = List.of(
            new Rule("minLength", null, "At least " + MIN_LENGTH + " characters"),
            new Rule("uppercase", UPPERCASE, "One uppercase letter (A-Z)"),
            new Rule("lowercase", LOWERCASE, "One lowercase letter (a-z)"),
            new Rule("digit", DIGIT, "One number (0-9)"),
            new Rule("special", SPECIAL, "One special character (e.g. ! @ # $ % & *)")
    );

    private PasswordPolicy() {
    }

    /**
     * @return the rules the given password fails, empty when it satisfies every rule.
     */
    public static List<Rule> failures(String password) {
        if (password == null || password.isEmpty()) {
            return RULES;
        }

        List<Rule> failed = new java.util.ArrayList<>();

        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            failed.add(RULES.get(0));
        }

        for (int i = 1; i < RULES.size(); i++) {
            if (!RULES.get(i).pattern().matcher(password).matches()) {
                failed.add(RULES.get(i));
            }
        }

        return failed;
    }

    public static boolean isValid(String password) {
        return failures(password).isEmpty();
    }

    /** e.g. "At least 8 characters, One uppercase letter (A-Z)" */
    public static String describe() {
        StringBuilder sb = new StringBuilder();
        for (Rule rule : RULES) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(rule.requirement());
        }
        return sb.toString();
    }
}