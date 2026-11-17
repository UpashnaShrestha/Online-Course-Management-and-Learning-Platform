package com.college.OnlineCourseManagement.LearningPlatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * CORS configuration that enables cross-origin requests from the frontend development server.
 * 
 * Allows both http://localhost:5173 and http://127.0.0.1:5173 (browsers treat these as
 * different origins). Also permits credentials (HttpOnly cookies for refresh tokens).
 */
@Configuration
public class CorsConfig {

    private static final String[] DEFAULT_ALLOWED_ORIGINS = {
            "http://localhost:5173",
            "http://127.0.0.1:5173"
    };

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns(DEFAULT_ALLOWED_ORIGINS)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        // Sends the refresh token as an HttpOnly cookie, so credentials are
                        // required. Origin patterns (not wildcard) are used because the wildcard
                        // cannot be combined with allowCredentials.
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}