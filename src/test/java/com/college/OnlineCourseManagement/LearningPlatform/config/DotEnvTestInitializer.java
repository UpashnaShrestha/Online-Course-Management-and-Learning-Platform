package com.college.OnlineCourseManagement.LearningPlatform.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Makes the real {@code .env} visible to {@code @SpringBootTest} contexts.
 *
 * {@code @SpringBootTest} builds its own SpringApplication and never calls
 * {@code Application.main}, so it bypasses the {@link DotEnv} hook. Without this the tests
 * would run with blank SMTP settings and pass for the wrong reason.
 *
 * Reference it with {@code @ContextConfiguration(initializers = DotEnvTestInitializer.class)}.
 */
public class DotEnvTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        DotEnv.registerOnce(applicationContext.getEnvironment());
    }
}