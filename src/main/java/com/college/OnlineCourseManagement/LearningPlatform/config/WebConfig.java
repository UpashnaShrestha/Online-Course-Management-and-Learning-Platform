package com.college.OnlineCourseManagement.LearningPlatform.config;

import com.college.OnlineCourseManagement.LearningPlatform.service.ProfileImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves uploaded images back to the browser.
 *
 * <p>Avatars are fetched by {@code <img src>}, which cannot attach an Authorization header, so
 * they have to be reachable without a token. SecurityConfig permits GET on this prefix only.
 *
 * <p>The location is the upload <em>root</em>, not the image sub-directory. The pattern
 * "/uploads/**" already swallows the sub-directory that appears in the stored path
 * ("/uploads/profile-images/x.png"), so pointing the location at the sub-directory makes Spring
 * look for "profile-images/profile-images/x.png" and every avatar 404s.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ProfileImageStorage storage;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(storage.publicPath() + "/**")
                .addResourceLocations(storage.rootDirectory().toUri().toString())
                .setCachePeriod(3600);
    }
}