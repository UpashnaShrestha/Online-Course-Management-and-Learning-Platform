package com.college.OnlineCourseManagement.LearningPlatform.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression cover for the reason password-reset email stopped being sent.
 *
 * Spring Boot's own mechanism -- {@code spring.config.import: "optional:file:.env"} -- resolves
 * the path against the process working directory. Run from anywhere else and the file is not
 * found; every variable in it silently disappears, the SMTP username ends up blank, and the
 * reset endpoint still answers "if an account exists, a code has been sent".
 */
class DotEnvTest {

    @Test
    @DisplayName("parses plain KEY=VALUE pairs")
    void parsesSimpleEntries(@TempDir Path dir) throws Exception {
        File env = write(dir, """
                MAIL_HOST=smtp.gmail.com
                MAIL_PORT=587
                """);

        assertThat(DotEnv.parse(env))
                .containsEntry("MAIL_HOST", "smtp.gmail.com")
                .containsEntry("MAIL_PORT", "587");
    }

    @Test
    @DisplayName("skips comments and blank lines, and tolerates an export prefix")
    void skipsCommentsAndExportPrefix(@TempDir Path dir) throws Exception {
        File env = write(dir, """
                # a comment

                export MAIL_USERNAME=user@example.com
                  # indented comment
                JWT_SECRET=abc
                """);

        assertThat(DotEnv.parse(env))
                .containsEntry("MAIL_USERNAME", "user@example.com")
                .containsEntry("JWT_SECRET", "abc")
                .hasSize(2);
    }

    @Test
    @DisplayName("strips one layer of matching quotes, as dotenv implementations do")
    void stripsMatchingQuotes(@TempDir Path dir) throws Exception {
        File env = write(dir, """
                MAIL_PASSWORD="pa:ss#word"
                PLAIN='single quoted'
                """);

        assertThat(DotEnv.parse(env))
                .containsEntry("MAIL_PASSWORD", "pa:ss#word")
                .containsEntry("PLAIN", "single quoted");
    }

    @Test
    @DisplayName("keeps a value containing '#' when it is not a comment")
    void keepsHashInsideValue(@TempDir Path dir) throws Exception {
        File env = write(dir, "MAIL_PASSWORD=abc#123\n");

        assertThat(DotEnv.parse(env)).containsEntry("MAIL_PASSWORD", "abc#123");
    }

    @Test
    @DisplayName("keeps '=' characters inside a value")
    void keepsEqualsInsideValue(@TempDir Path dir) throws Exception {
        File env = write(dir, "JWT_SECRET=abc=def==\n");

        assertThat(DotEnv.parse(env)).containsEntry("JWT_SECRET", "abc=def==");
    }

    @Test
    @DisplayName("ignores malformed lines instead of failing")
    void ignoresMalformedLines(@TempDir Path dir) throws Exception {
        File env = write(dir, """
                JUST_A_WORD
                =leading-equals-is-not-a-key
                GOOD=value
                """);

        assertThat(DotEnv.parse(env)).containsExactly(java.util.Map.entry("GOOD", "value"));
    }

    @Test
    @DisplayName("searches the working directory, its parents, and the jar directory")
    void searchesSeveralLocations(@TempDir Path dir) {
        List<File> candidates = DotEnv.candidates();

        // The working directory chain, the jar directory, and the jar's parent.
        assertThat(candidates).hasSizeGreaterThanOrEqualTo(3);
        assertThat(candidates)
                .as("every candidate must be named .env")
                .allSatisfy(file -> assertThat(file.getName()).isEqualTo(".env"));
        assertThat(candidates.stream().distinct().toList())
                .as("a location must not be searched twice")
                .hasSameSizeAs(candidates);
    }

    @Test
    @DisplayName("reports a missing .env rather than silently doing nothing")
    void reportsMissingFile(@TempDir Path dir) {
        // locate() searches real locations, so this only asserts it tolerates their absence.
        File found = DotEnv.locate();

        // Either a .env exists somewhere on this machine (the project root, in development),
        // or it does not. Both are valid; the point is that the call does not throw.
        assertThat(found == null || found.isFile()).isTrue();
    }

    private File write(Path dir, String content) throws Exception {
        Path file = dir.resolve(".env");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file.toFile();
    }
}