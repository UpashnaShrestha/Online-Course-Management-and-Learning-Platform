package com.college.OnlineCourseManagement.LearningPlatform.controller;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.CertificateResponse;
import com.college.OnlineCourseManagement.LearningPlatform.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @GetMapping("/api/student/certificates")
    public ResponseEntity<ApiResponse<List<CertificateResponse>>> getCertificates() {
        return ResponseEntity.ok(ApiResponse.success(
                "Certificates fetched successfully",
                certificateService.getStudentCertificates()
        ));
    }

    @GetMapping("/api/certificates/{id}")
    public ResponseEntity<ApiResponse<CertificateResponse>> getCertificate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Certificate fetched successfully",
                certificateService.getCertificate(id)
        ));
    }

    @GetMapping("/api/certificates/{id}/download")
    public ResponseEntity<Resource> downloadCertificatePdf(@PathVariable Long id) {
        return certificateService.downloadCertificatePdf(id);
    }
}