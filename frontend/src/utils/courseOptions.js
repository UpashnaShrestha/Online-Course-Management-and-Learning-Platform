/**
 * The level and duration values a course may declare.
 *
 * Mirrors CourseLevel.java and CourseDuration.java, and the instructor course form prefers
 * `GET /api/courses/options` (which is served from those same enums) so the dropdown and the
 * backend validation cannot disagree. These constants are the offline fallback used when
 * that request fails.
 */

/** Mirrors com.college...enums.CourseLevel. Order is the dropdown order. */
export const COURSE_LEVELS = ['Beginner', 'Intermediate', 'Advanced']

/** Mirrors com.college...enums.CourseDuration. Order is shortest first. */
export const COURSE_DURATIONS = [
  'Under 1 hour',
  '1-3 hours',
  '3-6 hours',
  '6-10 hours',
  '10-20 hours',
  '20-40 hours',
  '1-4 weeks',
  '4-8 weeks',
  '8-12 weeks',
  '3-6 months',
  '6+ months',
]

/**
 * Normalises a stored level for display and for filter matching.
 *
 * Older rows may hold enum-style uppercase values ("BEGINNER") while the dropdowns use the
 * display label ("Beginner"). Comparing raw strings made "BEGINNER" and "Beginner" look like
 * two different levels in the catalog filter, so both are folded to one canonical form.
 */
export function normalizeLevel(level) {
  if (!level) return null

  const wanted = String(level).trim().toLowerCase()

  return (
    COURSE_LEVELS.find((option) => option.toLowerCase() === wanted) ??
    // Tolerate separators used by older free-text values: "beginner-level", "beginner level".
    COURSE_LEVELS.find((option) =>
      wanted.startsWith(option.toLowerCase().split(' ')[0])
    ) ??
    null
  )
}

/** Display label for a stored level, or null when unrecognised. */
export function levelLabel(level) {
  return normalizeLevel(level)
}

/**
 * Whether a stored duration is still one of the offered options.
 *
 * Returns the canonical option, or the original text when it is not recognised so legacy
 * values keep rendering rather than disappearing from the page.
 */
export function normalizeDuration(duration) {
  if (!duration) return null

  const trimmed = String(duration).trim()
  const wanted = trimmed.toLowerCase().replace(/\s*to\s*/g, '-').replace(/\s+/g, ' ')

  return (
    COURSE_DURATIONS.find((option) => option.toLowerCase() === wanted) ??
    COURSE_DURATIONS.find((option) => option.toLowerCase().replace(/\s+/g, '') === wanted.replace(/\s+/g, '')) ??
    trimmed
  )
}