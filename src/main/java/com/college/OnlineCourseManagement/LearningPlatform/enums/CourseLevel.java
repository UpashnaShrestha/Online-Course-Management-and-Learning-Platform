package com.college.OnlineCourseManagement.LearningPlatform.enums;

/**
 * The course difficulty levels the platform recognises.
 *
 * This used to be free text, which let "beginner", "Beginner" and "BEGINNER" coexist in the
 * database and show up as three separate entries in the catalog's level filter. A closed set
 * makes that impossible.
 *
 * The stored value is the enum name (see the @Enumerated mapping on Course), so existing
 * rows must already be uppercase -- they are, except for lowercase junk which is normalised
 * before this was introduced.
 */
public enum CourseLevel {

    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced");

    private final String label;

    CourseLevel(String label) {
        this.label = label;
    }

    /** Title-case value for display in the UI. */
    public String getLabel() {
        return label;
    }

    /**
     * Lenient lookup used when accepting input from a form or an API client.
     *
     * @return the matching level, or null when the text is not recognised.
     */
    public static CourseLevel fromText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();

        for (CourseLevel level : values()) {
            if (level.name().equalsIgnoreCase(trimmed) || level.label.equalsIgnoreCase(trimmed)) {
                return level;
            }
        }

        // Tolerate the suffixes and prefixes older free-text rows accumulated, such as
        // "beginner-level" or "advanced track". Match on the leading word only, so
        // "beginners" still fails -- that is a different word, not a decorated level.
        String[] words = trimmed.split("[^A-Za-z]+");
        String leadingWord = words.length > 0 ? words[0] : "";
        if (!leadingWord.isEmpty()) {
            for (CourseLevel level : values()) {
                if (level.name().equalsIgnoreCase(leadingWord)
                        || level.label.equalsIgnoreCase(leadingWord)) {
                    return level;
                }
            }
        }

        return null;
    }
}