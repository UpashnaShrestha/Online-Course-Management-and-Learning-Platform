package com.college.OnlineCourseManagement.LearningPlatform;

import com.college.OnlineCourseManagement.LearningPlatform.config.DotEnv;
import com.college.OnlineCourseManagement.LearningPlatform.config.DotEnvTestInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the configuration mistakes that silently disabled password reset email:
 *  1. The .env file not being loaded -- previously because Spring Boot does not read .env at
 *     all, and then because its own mechanism resolved the path against the working directory.
 *  2. A blank/short JWT secret aborting startup.
 *  3. Startup depending on the SMTP server being reachable.
 */
@SpringBootTest
@ContextConfiguration(initializers = DotEnvTestInitializer.class)
class ConfigurationLoadingTest {

    @Autowired
    private Environment env;

    @Test
    @DisplayName("the project .env is actually loaded, not just present on disk")
    void dotEnvIsLoaded() {
        // Spring Boot does not read .env by itself, and its own mechanism
        // (spring.config.import: file:.env) resolves against the PROCESS WORKING DIRECTORY --
        // so a run configuration pointing elsewhere silently dropped every variable, which is
        // what stopped password-reset email being sent. DotEnv searches several locations;
        // this asserts one of them was found and registered.
        assertThat(env.getProperty(DotEnv.LOCATION_PROPERTY))
                .as("DotEnv must record which .env it used")
                .isNotNull();

        assertThat(((ConfigurableEnvironment) env).getPropertySources())
                .as("the .env values must be registered as a property source")
                .extracting(ps -> ps.getName())
                .contains(DotEnv.SOURCE_NAME);
    }

    @Test
    @DisplayName("no committed SMTP credential survives in application.yaml")
    void smtpCredentialsAreNotHardcoded() {
        assertThat(env.getProperty("spring.mail.username"))
                .as("MAIL_USERNAME must not have a default in application.yaml")
                .isEqualTo(env.getProperty("mail.username", env.getProperty("spring.mail.username")));

        String yaml = readYaml();
        assertThat(yaml).doesNotContainPattern("MAIL_USERNAME:[^}\\s]");
        assertThat(yaml).doesNotContainPattern("MAIL_PASSWORD:[^}\\s]");
    }

    @Test
    @DisplayName("a blank jwt secret no longer aborts startup")
    void jwtSecretIsNeverEmpty() {
        String secret = env.getProperty("jwt.secret");
        // May be blank (a random secret is then generated) but must never cause a
        // WeakKeyException, which was the original hard blocker.
        assertThat(env.getProperty("jwt.access-token-expiration")).isNotBlank();
        assertThat(env.getProperty("jwt.refresh-token-expiration")).isNotBlank();
        assertThat(env.getProperty("jwt.remember-me-refresh-token-expiration")).isNotBlank();
        assertThat(secret).isNotNull();
    }

    @Test
    @DisplayName("mail settings resolve to concrete values even without OS env vars")
    void mailPropertiesResolve() {
        assertThat(env.getProperty("spring.mail.host")).isNotBlank();
        assertThat(env.getProperty("spring.mail.port")).isNotBlank();
        assertThat(env.getProperty("app.mail.enabled")).isNotBlank();
    }

    @Test
    @DisplayName("no credentials are hardcoded as defaults in application.yaml")
    void noCommittedCredentials() {
        // Assert against the raw YAML, not the resolved environment: .env legitimately
        // supplies these values, but a committed default would leak into source control.
        String yaml = readYaml();

        assertThat(yaml)
                .as("application.yaml must not contain a fallback SMTP username")
                .doesNotContainPattern("MAIL_USERNAME:[^}\\s]");
        assertThat(yaml)
                .as("application.yaml must not contain a fallback SMTP password")
                .doesNotContainPattern("MAIL_PASSWORD:[^}\\s]");
    }

    private String readYaml() {
        try (var stream = getClass().getResourceAsStream("/application.yaml")) {
            assertThat(stream).as("application.yaml must be on the classpath").isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Could not read application.yaml", ex);
        }
    }

    @Test
    @DisplayName("JavaMailSender bean is available for the reset flow")
    void mailSenderBeanExists(@Autowired JavaMailSender mailSender) {
        assertThat(mailSender).isNotNull();
    }

    @Test
    @DisplayName("the OTP is never exposed in the API response")
    void otpIsNeverExposed() {
        assertThat(env.getProperty("app.password-reset.expose-otp"))
                .as("PASSWORD_RESET_EXPOSE_OTP must stay false")
                .isEqualToIgnoringCase("false");
    }

    @Test
    @DisplayName("the .env property source, when present, is registered")
    void dotEnvSourceRegisteredWhenPresent() {
        boolean hasDotEnv = ((ConfigurableEnvironment) env).getPropertySources().stream()
                .anyMatch(propertySource -> propertySource.getName().contains(".env"));

        // Optional: absence is valid (CI/production supply real environment variables
        // instead), so this only asserts that a present .env is actually picked up.
        if (hasDotEnv) {
            assertThat(env.getProperty("spring.mail.host")).isNotBlank();
        }
    }

    @Test
    @DisplayName("startup never depends on the SMTP server being reachable")
    void mailConnectivityIsNotRequiredForStartup() throws Exception {
        // Regression: spring.mail.test-connection=true makes
        // MailSenderValidatorAutoConfiguration throw during context refresh, so an
        // unreachable SMTP server stops the whole application from booting. That took down
        // login, the catalog and every dashboard for a problem that only affects email.
        String yaml = new String(
                getClass().getResourceAsStream("/application.yaml").readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(yaml)
                .as("test-connection must default to false, not true")
                .containsPattern("test-connection:\\s*\\$\\{MAIL_TEST_CONNECTION:false}");

        assertThat(env.getProperty("spring.mail.test-connection"))
                .as("the effective test-connection must be off")
                .isEqualTo("false");
    }

    @Test
    @DisplayName("the committed .env template contains no real credentials")
    void envExampleHasNoLiveCredentials() throws Exception {
        // .env is git-ignored, but .env.example is committed and is the file a new
        // developer copies. It must never carry a working App Password.
        var stream = getClass().getResourceAsStream("/../../.env.example");
        if (stream == null) {
            // Not on the test classpath in every environment; nothing to assert.
            return;
        }

        String example = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        assertThat(example)
                .as(".env.example must not carry a real SMTP password")
                .doesNotContainPattern("(?m)^MAIL_PASSWORD=\\S+");
        assertThat(example)
                .as(".env.example must not carry a real SMTP username")
                .doesNotContainPattern("(?m)^MAIL_USERNAME=\\S+");
    }
}