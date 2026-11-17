package com.college.OnlineCourseManagement.LearningPlatform.controller;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.ReviewRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.ReviewResponse;
import com.college.OnlineCourseManagement.LearningPlatform.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/courses/{id}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Review created successfully",
                        reviewService.createReview(id, request)
                ));
    }

    @GetMapping("/api/courses/{id}/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviews(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Reviews fetched successfully",
                reviewService.getCourseReviews(id)
        ));
    }
}
