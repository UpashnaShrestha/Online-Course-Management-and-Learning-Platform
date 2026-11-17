package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.LoginRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.LoginResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.RefreshTokenRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.RegisterRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.UserResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.NotificationType;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.DuplicateResourceException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.UnauthorizedException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.UserRepository;
import com.college.OnlineCourseManagement.LearningPlatform.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        notificationService.notify(
                savedUser,
                "Welcome to CourseFlow",
                "Your account is ready. Browse the catalog and enroll in your first course.",
                NotificationType.SYSTEM,
                "/courses"
        );
        emailService.sendWelcome(savedUser.getEmail(), savedUser.getFirstName());
        return UserMapper.toResponse(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = request.isRememberMe()
                ? jwtService.generateRememberMeRefreshToken(userDetails)
                : jwtService.generateRefreshToken(userDetails);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(UserMapper.toResponse(user))
                .build();
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();

            if (!jwtService.isRefreshToken(refreshToken)) {
                throw new UnauthorizedException("Invalid refresh token");
            }

            String username = jwtService.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                throw new UnauthorizedException("Refresh token is expired or invalid");
            }

            String newAccessToken = jwtService.generateAccessToken(userDetails);
            String newRefreshToken = jwtService.generateRefreshToken(userDetails);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

            return LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getAccessTokenExpiration())
                    .user(UserMapper.toResponse(user))
                    .build();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid refresh token");
        }
    }
}
