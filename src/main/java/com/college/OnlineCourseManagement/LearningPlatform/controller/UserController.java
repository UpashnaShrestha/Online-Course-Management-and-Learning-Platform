package com.college.OnlineCourseManagement.LearningPlatform.controller;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.ChangePasswordRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.UpdateProfileRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.UserResponse;
import com.college.OnlineCourseManagement.LearningPlatform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse user = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("User profile fetched successfully", user));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "User profile updated successfully",
                userService.updateCurrentUser(request)
        ));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    /**
     * Uploads a profile picture from the user's device.
     *
     * <p>Separate from PUT /me because that endpoint takes JSON, and a multipart body needs a
     * different content type. The browser resizes the photo before sending it, so this receives
     * something avatar-sized rather than a multi-megabyte phone original.
     *
     * <p>The stored {@code profileImage} is a host-independent path such as
     * {@code /uploads/profile-images/x.png}, the same as everywhere else in the API. The client
     * resolves it against the API base URL; see {@code resolveImageUrl} in utils/format.js.
     */
    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> uploadProfileImage(
            @RequestParam("file") MultipartFile file
    ) {
        UserResponse user = userService.updateProfileImage(file);
        return ResponseEntity.ok(ApiResponse.success("Profile image uploaded successfully", user));
    }

    /** Clears the profile picture and deletes the stored file. */
    @DeleteMapping("/me/profile-image")
    public ResponseEntity<ApiResponse<UserResponse>> deleteProfileImage() {
        userService.removeProfileImage();
        return ResponseEntity.ok(ApiResponse.success(
                "Profile image removed successfully", userService.getCurrentUser()));
    }
}