package com.college.OnlineCourseManagement.LearningPlatform.enums;

import java.util.List;

/**
 * The durations a course may declare.
 *
 * Duration was previously free text, which produced near-duplicates ("6 weeks", "6 Weeks",
 * "1 month", "30 days") and made two courses with the same length look different. A closed set
 * keeps the catalog tidy and lets the UI render a real dropdown instead of a text box.
 *
 * {@link #matches(String)} still tolerates anything the catalog might already contain, so
 * existing rows keep rendering even if they predate this enum.
 */
public enum CourseDuration {

    UNDER_1_HOUR("Under 1 hour", 1, "hour"),
    HOURS_1_3("1-3 hours", 3, "hours"),
    HOURS_3_6("3-6 hours", 6, "hours"),
    HOURS_6_10("6-10 hours", 10, "hours"),
    HOURS_10_20("10-20 hours", 20, "hours"),
    HOURS_20_40("20-40 hours", 40, "hours"),
    WEEKS_1_4("1-4 weeks", 4, "weeks"),
    WEEKS_4_8("4-8 weeks", 8, "weeks"),
    WEEKS_8_12("8-12 weeks", 12, "weeks"),
    MONTHS_3_6("3-6 months", 6, "months"),
    MONTHS_6_PLUS("6+ months", 0, "months");

    private final String label;
    private final int sortBound;
    private final String unit;

    CourseDuration(String label, int sortBound, String unit) {
        this.label = label;
        this.sortBound = sortBound;
        this.unit = unit;
    }

    /** Value shown in the dropdown, e.g. "4-8 weeks". This is what gets stored. */
    public String getLabel() {
        return label;
    }

    public String getUnit() {
        return unit;
    }

    /**
     * Approximate length in hours, or 0 when unknown. Useful for sorting and for "shortest
     * course" style filters without parsing the label.
     */
    /**
     * Approximate length in hours, used for sorting.
     *
     * Weeks and months continue past the "20-40 hours" band, so the values must keep
     * increasing past 40 or a course advertised as 8-12 weeks would sort as the shortest.
     * A week of study material is treated as ~12 hours of video and exercises.
     */
    public int getApproximateHours() {
        return switch (this) {
            case UNDER_1_HOUR -> 1;
            case HOURS_1_3 -> 3;
            case HOURS_3_6 -> 6;
            case HOURS_6_10 -> 10;
            case HOURS_10_20 -> 20;
            case HOURS_20_40 -> 40;
            // Lower bound of each band, at roughly 12 hours of study per week.
            case WEEKS_1_4 -> 48;
            case WEEKS_4_8 -> 96;
            case WEEKS_8_12 -> 192;
            case MONTHS_3_6 -> 432;
            case MONTHS_6_PLUS -> 864;
        };
    }

    /**
     * Lenient lookup for values arriving from a form, an API client or an older row.
     *
     * @return the matching duration, or null when the text is not recognised.
     */
    public static CourseDuration fromText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();

        for (CourseDuration duration : values()) {
            if (duration.label.equalsIgnoreCase(trimmed) || duration.name().equalsIgnoreCase(trimmed)) {
                return duration;
            }
        }

        // Normalised comparison so "6 - 10 Hours", "6-10 hours" and "6 to 10 hours" all match.
        String normalised = trimmed.toLowerCase().replace(" to ", "-").replace(" ", "").replace("--", "-");
        for (CourseDuration duration : values()) {
            String candidate = duration.label.toLowerCase().replace(" ", "");
            if (candidate.equals(normalised)) {
                return duration;
            }
        }

        return null;
    }

    /**
     * Whether an existing stored duration is still one of the offered options. Used by tests
     * and by the admin tooling to spot rows that should be migrated.
     */
    public static boolean matches(String value) {
        return fromText(value) != null;
    }

    /** All options in the order they should appear in a dropdown. */
    public static List<CourseDuration> options() {
        return List.of(values());
    }
}