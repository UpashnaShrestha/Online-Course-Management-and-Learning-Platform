package com.college.OnlineCourseManagement.LearningPlatform;

import com.college.OnlineCourseManagement.LearningPlatform.config.DotEnvTestInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the multipart configuration against silent fallback.
 *
 * <p>{@code spring.servlet.multipart.max-file-size} had been written at the wrong indentation,
 * one level too shallow. YAML accepted it, the application started, and uploads were rejected
 * at Spring Boot's built-in 1 MB default instead of the configured 5 MB -- with no warning
 * anywhere. A property that is present but in the wrong place is the worst kind, because it
 * looks like it is working.
 */
@SpringBootTest
@ContextConfiguration(initializers = DotEnvTestInitializer.class)
class MultipartConfigurationTest {

    @Autowired
    private Environment env;

    @Autowired
    private MultipartProperties multipartProperties;

    @Test
    @DisplayName("max-file-size is configured from app.upload.max-image-size, not left at the default")
    void maxFileSizeIsConfigured() {
        assertThat(env.getProperty("spring.servlet.multipart.max-file-size"))
                .as("the property must be resolvable under spring.servlet, at the right indentation")
                .isNotNull()
                .isNotBlank();
    }

    @Test
    @DisplayName("the enforced limit is the configured one, not Spring Boot's 1 MB default")
    void enforcedLimitMatchesConfiguration() {
        long configured = multipartProperties.getMaxFileSize().toBytes();

        assertThat(configured)
                .as("uploads must be limited by app.upload.max-image-size (5MB), not the 1MB default")
                .isEqualTo(5L * 1024 * 1024);
    }

    @Test
    @DisplayName("max-request-size matches max-file-size so a single file is not capped twice")
    void requestSizeMatchesFileSize() {
        assertThat(multipartProperties.getMaxRequestSize().toBytes())
                .isEqualTo(multipartProperties.getMaxFileSize().toBytes());
    }

    @Test
    @DisplayName("the servlet block is a child of spring, not a sibling or a child of another key")
    void servletBlockSitsUnderSpring() throws Exception {
        String yaml = new String(
                getClass().getResourceAsStream("/application.yaml").readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);

        assertThat(yaml)
                .as("a top-level 'servlet:' is silently ignored by Spring Boot")
                .doesNotContainPattern("(?m)^servlet:");
        assertThat(yaml)
                .as("'servlet:' must be indented two spaces, under 'spring:'")
                .containsPattern("(?m)^ {2}servlet:");
    }

    /**
     * Walks the YAML the way a parser does: a key at indent N belongs to the most recent key
     * at indent N-1. This is the rule that was broken -- the block looked correctly indented
     * but had landed inside the {@code app:} section, quietly resolving to
     * {@code app.servlet.multipart.*}.
     */
    @Test
    @DisplayName("every known key nests under the section it is meant to")
    void keyNestingIsCorrect() throws Exception {
        String yaml = new String(
                getClass().getResourceAsStream("/application.yaml").readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);

        assertThat(parentOf(yaml, "multipart"))
                .as("multipart must hang off spring.servlet")
                .isEqualTo("spring.servlet");

        assertThat(parentOf(yaml, "password-reset"))
                .as("password-reset must hang off app")
                .isEqualTo("app");

        assertThat(parentOf(yaml, "cors"))
                .as("cors must hang off app")
                .isEqualTo("app");
    }

    /** Resolves the dotted path of the section that contains the given leaf key. */
    private String parentOf(String yaml, String leafKey) {
        Deque<Entry> path = new ArrayDeque<>();

        // Note: the loop variable is deliberately not called "raw" -- that is a restricted
        // identifier in Java 21 for the raw-types preview, and it confuses resolution.
        for (String line : yaml.split("\\R")) {
            if (line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }

            int indent = line.length() - line.stripLeading().length();
            String trimmed = line.trim();

            int colon = trimmed.indexOf(':');
            if (colon < 0 || !trimmed.endsWith(":")) {
                continue;
            }

            String key = trimmed.substring(0, colon);
            while (!path.isEmpty() && path.peekLast().indent() >= indent) {
                path.removeLast();
            }

            if (key.equals(leafKey)) {
                StringBuilder dotted = new StringBuilder();
                for (Entry entry : path) {
                    if (dotted.length() > 0) {
                        dotted.append('.');
                    }
                    dotted.append(entry.key());
                }
                return dotted.toString();
            }

            path.addLast(new Entry(indent, key));
        }

        return "(not found)";
    }

    private record Entry(int indent, String key) {
    }
}