package com.college.OnlineCourseManagement.LearningPlatform.controller;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.AccountDeletionRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.UserResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.AdminUserDetailResponse;
import com.college.OnlineCourseManagement.LearningPlatform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(
                "Users fetched successfully",
                userService.getAllUsers()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getDetails(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "User details fetched successfully",
                userService.getAdminUserDetails(id)
        ));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> setStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                request.enabled() ? "User enabled successfully" : "User disabled successfully",
                userService.setEnabled(id, request.enabled())
        ));
    }

    public record UserStatusRequest(@NotNull Boolean enabled) {
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @Valid @RequestBody AccountDeletionRequest request
    ) {
        userService.deleteUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User account deleted successfully"));
    }
}