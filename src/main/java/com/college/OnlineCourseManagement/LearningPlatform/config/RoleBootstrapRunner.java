package com.college.OnlineCourseManagement.LearningPlatform.config;

import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration always creates STUDENTs, so administrator and instructor accounts have to be
 * seeded. Without this, /api/admin/** and /api/instructor/** are unreachable on a fresh
 * database. Accounts are only created when the username does not already exist, so this is
 * safe to run on every boot.
 *
 * Configure via APP_BOOTSTRAP_ADMIN_USERNAME / APP_BOOTSTRAP_ADMIN_PASSWORD and
 * APP_BOOTSTRAP_INSTRUCTOR_USERNAME / APP_BOOTSTRAP_INSTRUCTOR_PASSWORD. Leave a password
 * blank to skip that role entirely.
 */
@Component
@RequiredArgsConstructor
public class RoleBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-username:}")
    private String adminUsername;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    @Value("${app.bootstrap.instructor-username:}")
    private String instructorUsername;

    @Value("${app.bootstrap.instructor-password:}")
    private String instructorPassword;

    @Override
    @Transactional
    public void run(String... args) {
        seed(adminUsername, adminPassword, Role.ADMIN, "Admin", "User");
        seed(instructorUsername, instructorPassword, Role.INSTRUCTOR, "Seed", "Instructor");
    }

    private void seed(String username, String password, Role role, String firstName, String lastName) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return;
        }

        if (userRepository.existsByUsername(username.trim())) {
            return;
        }

        User user = User.builder()
                .username(username.trim())
                .firstName(firstName)
                .lastName(lastName)
                .email(username.trim().toLowerCase() + "@bootstrap.local")
                .password(passwordEncoder.encode(password))
                .role(role)
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("Seeded {} account '{}'", role, username.trim());
    }
}