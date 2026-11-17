package com.college.OnlineCourseManagement.LearningPlatform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the project's {@code .env} file into the Spring environment.
 *
 * Spring Boot does not read {@code .env} on its own, and its own answer -- adding
 * {@code spring.config.import: "optional:file:.env[.properties]"} -- resolves the path against
 * the <em>process working directory</em>. A run configuration pointing anywhere other than the
 * project root therefore dropped every variable in the file silently. The most visible symptom
 * was password-reset email not being sent: the endpoint still replied "if an account exists, a
 * reset code has been sent", no mail arrived, and the only clue was a WARN in the startup log.
 *
 * So instead of relying on one relative path, several locations are tried in order:
 *
 * <ol>
 *   <li>the working directory and a few parents up -- what {@code mvn spring-boot:run} uses;</li>
 *   <li>the directory holding the packaged jar and its parent -- so {@code java -jar} works
 *       when launched from anywhere.</li>
 * </ol>
 *
 * The resolved path is logged at startup and exposed as {@code app.dotenv.location}, so the
 * answer to "is my .env being read?" is visible rather than inferred.
 *
 * The source is added <em>last</em>: real environment variables, {@code -D} flags and command
 * line arguments all still take precedence over the file.
 */
public final class DotEnv {

    private static final Logger log = LoggerFactory.getLogger(DotEnv.class);

    /** Name of the property source, so repeated calls do not stack up duplicates. */
    public static final String SOURCE_NAME = "courseflow-dotenv";

    /** Absolute path of the file that was used, or "(none)". */
    public static final String LOCATION_PROPERTY = "app.dotenv.location";

    /** How far up from the working directory to look before giving up. */
    private static final int MAX_PARENT_DEPTH = 5;

    private DotEnv() {
    }

    /**
     * Builds the environment the application will run with, with {@code .env} registered.
     *
     * Called from {@code Application.main} and handed to {@code SpringApplication.setEnvironment}.
     *
     * @return an environment containing the discovered {@code .env} values.
     */
    public static ConfigurableEnvironment environmentWithEnvFile() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        applyTo(environment);
        return environment;
    }

    /**
     * Same as {@link #environmentWithEnvFile()} but idempotent, so tests can call it to make
     * the real {@code .env} visible to a {@code @SpringBootTest} context -- which never passes
     * through {@code Application.main}.
     */
    public static ConfigurableEnvironment registerOnce(ConfigurableEnvironment environment) {
        if (!environment.getPropertySources().contains(SOURCE_NAME)) {
            applyTo(environment);
        } else {
            environment.getSystemProperties().putIfAbsent(LOCATION_PROPERTY, "(already loaded)");
        }
        return environment;
    }

    /**
     * Registers the discovered {@code .env} as a property source.
     *
     * @return the file that was used, or null when there was none.
     */
    public static File applyTo(ConfigurableEnvironment environment) {
        File envFile = locate();

        // Record the outcome either way, so the startup banner can show it.
        environment.getSystemProperties().put(LOCATION_PROPERTY,
                envFile == null ? "(none)" : envFile.getAbsolutePath());

        if (envFile == null) {
            log.info("No .env file found; falling back to OS environment variables and -D flags");
            return null;
        }

        Map<String, Object> values = parse(envFile);
        if (values.isEmpty()) {
            log.warn("{} contained no KEY=VALUE entries", envFile.getAbsolutePath());
            return envFile;
        }

        // addLast: OS environment variables and -D flags must still win over the file.
        environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, values));
        log.info("Loaded .env from {} ({} entries)", envFile.getAbsolutePath(), values.size());

        return envFile;
    }

    /** First readable {@code .env} among the candidates, or null when there is none. */
    static File locate() {
        List<File> tried = candidates();

        if (Boolean.getBoolean("courseflow.logDotEnvCandidates")) {
            log.info("Searching for .env in {} location(s)", tried.size());
            tried.forEach(file -> log.info("  candidate: {} (exists={})", file, file.isFile()));
            log.info("  code source: {}", codeSource());
        }

        for (File candidate : tried) {
            if (candidate.isFile() && candidate.canRead()) {
                return candidate;
            }
        }
        return null;
    }

    /** Raw location the classes were loaded from; useful when the search comes up empty. */
    private static String codeSource() {
        try {
            return DotEnv.class.getProtectionDomain().getCodeSource().getLocation().toString();
        } catch (Exception ex) {
            return "(unavailable: " + ex + ")";
        }
    }

    static List<File> candidates() {
        List<File> found = new ArrayList<>();

        // 1. The working directory and a few parents up.
        File directory = new File("").getAbsoluteFile();
        int depth = 0;
        while (directory != null && depth < MAX_PARENT_DEPTH) {
            found.add(new File(directory, ".env"));
            directory = directory.getParentFile();
            depth++;
        }

        // 2. Alongside the packaged jar and one level up.
        File jarDirectory = jarDirectory();
        if (jarDirectory != null) {
            found.add(new File(jarDirectory, ".env"));
            File parent = jarDirectory.getParentFile();
            if (parent != null) {
                found.add(new File(parent, ".env"));
            }
        }

        return found;
    }

    /**
     * Directory the application was loaded from, or null when it is not on disk.
     *
     * <p>Under a Spring Boot fat jar the code source is reported as a nested URL such as
     * {@code jar:file:/path/app.jar!/BOOT-INF/classes!/}, so the {@code file:} URI has to be
     * pulled out of it before the {@code .env} beside the jar can be found. Falling back to
     * scanning for the jar keeps this working when the URL shape changes again.
     */
    private static File jarDirectory() {
        File fromCodeSource = directoryFromCodeSource();
        return fromCodeSource != null ? fromCodeSource : directoryFromClasspathJar();
    }

    private static File directoryFromCodeSource() {
        try {
            String location = codeSource();
            // Reduce "jar:file:/x/app.jar!/BOOT-INF/classes!/" to "/x/app.jar".
            String uri = location;
            if (uri.startsWith("jar:")) {
                uri = uri.substring("jar:".length());
            }
            int nested = uri.indexOf("!/");
            if (nested > 0) {
                uri = uri.substring(0, nested);
            }
            if (!uri.startsWith("file:")) {
                return null;
            }
            uri = uri.substring("file:".length());

            File resolved = new File(new java.net.URI(uri));
            return resolved.isDirectory() ? resolved : resolved.getParentFile();
        } catch (Exception ex) {
            return null;
        }
    }

    /** Walks the JVM's classpath entries for the first jar, as a last resort. */
    private static File directoryFromClasspathJar() {
        String classpath = System.getProperty("java.class.path", "");
        for (String entry : classpath.split(java.io.File.pathSeparator)) {
            if (entry.endsWith(".jar") && !entry.endsWith("*-sources.jar")) {
                File jar = new File(entry);
                if (jar.isFile()) {
                    return jar.getParentFile();
                }
            }
        }
        return null;
    }

    /**
     * Parses {@code KEY=VALUE} lines, tolerating {@code export} prefixes, {@code #} comments
     * and one layer of matching quotes.
     *
     * <p>Deliberately hand-rolled rather than delegated to a YAML loader: a .env file is not
     * YAML, and values such as {@code pass:word} or an unquoted {@code #} would confuse one.
     */
    static Map<String, Object> parse(File file) {
        Map<String, Object> values = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new java.io.FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String entry = line.trim();

                if (entry.isEmpty() || entry.startsWith("#")) {
                    continue;
                }

                // "export KEY=value" is common in shell-flavoured .env files.
                if (entry.startsWith("export ")) {
                    entry = entry.substring("export ".length()).trim();
                }

                int separator = entry.indexOf('=');
                if (separator <= 0) {
                    continue;
                }

                String key = entry.substring(0, separator).trim();
                if (key.isEmpty()) {
                    continue;
                }

                values.put(key, stripQuotes(entry.substring(separator + 1).trim()));
            }
        } catch (IOException ex) {
            log.warn("Could not read {}: {}", file.getAbsolutePath(), ex.getMessage());
        }

        return values;
    }

    /** Removes one layer of matching quotes, as dotenv implementations do. */
    private static String stripQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}