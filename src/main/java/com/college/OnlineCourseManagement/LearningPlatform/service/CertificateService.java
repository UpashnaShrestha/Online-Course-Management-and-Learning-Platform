
package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.CertificateResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Certificate;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CertificateRepository;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final UserService userService;

    // GET ALL CERTIFICATES OF LOGGED-IN STUDENT
    @Transactional(readOnly = true)
    public List<CertificateResponse> getStudentCertificates() {

        User student = getStudent();

        return certificateRepository
                .findByStudentIdOrderByIssuedAtDesc(student.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // GET SINGLE CERTIFICATE
    @Transactional(readOnly = true)
    public CertificateResponse getCertificate(Long id) {

        User student = getStudent();

        Certificate certificate = certificateRepository
                .findByIdAndStudentId(id, student.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Certificate not found"));

        return toResponse(certificate);
    }

    // DOWNLOAD CERTIFICATE PDF
    @Transactional
    public ResponseEntity<Resource> downloadCertificatePdf(Long id) {

        User student = getStudent();

        Certificate certificate = certificateRepository
                .findByIdAndStudentId(id, student.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Certificate not found"));

        if (certificate.getPdfFilePath() == null
                || !pdfExists(certificate.getPdfFilePath())) {

            generateCertificatePdf(certificate);
        }

        Path pdfPath = Paths.get(certificate.getPdfFilePath());

        try {
            Resource resource = new UrlResource(pdfPath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException(
                        "Certificate PDF not found");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\""
                                    + certificate.getCertificateNumber()
                                    + ".pdf\""
                    )
                    .body(resource);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unable to download certificate PDF", e);
            throw new RuntimeException(
                    "Unable to download certificate PDF", e);
        }
    }

    // CHECK WHETHER PDF EXISTS
    private boolean pdfExists(String pdfFilePath) {

        if (pdfFilePath == null || pdfFilePath.isBlank()) {
            return false;
        }

        Path path = Paths.get(pdfFilePath);

        return Files.exists(path) && Files.isReadable(path);
    }

    // GENERATE CERTIFICATE PDF USING OPENPDF
    @Transactional
    public void generateCertificatePdf(Certificate certificate) {

        User student = certificate.getStudent();
        Course course = certificate.getCourse();

        Document document = new Document(PageSize.A4);

        try {
            Path certDir = Paths.get("certificates")
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(certDir);

            String fileName = "certificate_"
                    + student.getId()
                    + "_"
                    + course.getId()
                    + ".pdf";

            Path filePath = certDir.resolve(fileName);

            // Generate verification code only once
            if (certificate.getVerifiedCode() == null
                    || certificate.getVerifiedCode().isBlank()) {

                certificate.setVerifiedCode(
                        "VERIFY-"
                                + UUID.randomUUID()
                                .toString()
                                .substring(0, 8)
                                .toUpperCase()
                );
            }

            try (OutputStream outputStream =
                         Files.newOutputStream(filePath)) {

                PdfWriter.getInstance(document, outputStream);
                document.open();

                // Fonts
                Font titleFont = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 30, Font.BOLD);

                Font headingFont = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 22, Font.BOLD);

                Font nameFont = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 26, Font.BOLD);

                Font courseFont = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 18, Font.BOLD);

                Font textFont = FontFactory.getFont(
                        FontFactory.HELVETICA, 14, Font.NORMAL);

                Font smallFont = FontFactory.getFont(
                        FontFactory.HELVETICA, 10, Font.NORMAL);

                // Certificate content
                addCenteredParagraph(
                        document, "CourseFlow", titleFont, 25);

                addCenteredParagraph(
                        document, "CERTIFICATE OF COMPLETION",
                        headingFont, 45);

                addCenteredParagraph(
                        document,
                        "This certificate is proudly presented to",
                        textFont, 25);

                String studentName =
                        student.getFirstName() + " " + student.getLastName();

                addCenteredParagraph(
                        document, studentName, nameFont, 25);

                addCenteredParagraph(
                        document,
                        "For successfully completing the course:",
                        textFont, 15);

                addCenteredParagraph(
                        document, course.getTitle(), courseFont, 30);

                // Instructor name
                String instructorName = "N/A";

                if (course.getInstructors() != null
                        && !course.getInstructors().isEmpty()) {

                    User instructor =
                            course.getInstructors().iterator().next();

                    instructorName = instructor.getFirstName()
                            + " " + instructor.getLastName();
                }

                addCenteredParagraph(
                        document,
                        "Instructor: " + instructorName,
                        textFont, 15);

                addCenteredParagraph(
                        document,
                        "Completion Date: " + certificate.getIssuedAt(),
                        textFont, 25);

                addCenteredParagraph(
                        document,
                        "Certificate Number: "
                                + certificate.getCertificateNumber(),
                        smallFont, 10);

                addCenteredParagraph(
                        document,
                        "Verification Code: "
                                + certificate.getVerifiedCode(),
                        smallFont, 10);

                document.close();
            }

            // Save PDF path after successful generation
            certificate.setPdfFilePath(filePath.toString());
            certificateRepository.save(certificate);

            log.info("Generated certificate PDF: {}", filePath);

        } catch (Exception e) {

            if (document.isOpen()) {
                document.close();
            }

            log.error("Certificate PDF generation failed", e);

            throw new RuntimeException(
                    "Failed to generate PDF certificate", e);
        }
    }

    // HELPER METHOD FOR CENTERED TEXT
    private void addCenteredParagraph(
            Document document,
            String text,
            Font font,
            float spacingAfter) throws Exception {

        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingAfter(spacingAfter);

        document.add(paragraph);
    }

    // CONVERT CERTIFICATE ENTITY TO RESPONSE DTO
    private CertificateResponse toResponse(Certificate certificate) {

        return CertificateResponse.builder()
                .id(certificate.getId())
                .certificateNumber(certificate.getCertificateNumber())
                .courseId(certificate.getCourse().getId())
                .courseTitle(certificate.getCourse().getTitle())
                .issuedAt(certificate.getIssuedAt())
                .build();
    }

    // GET LOGGED-IN STUDENT
    private User getStudent() {

        User user = userService.getAuthenticatedUser();

        if (user.getRole() != Role.STUDENT) {
            throw new ForbiddenException(
                    "Only students can access their certificates");
        }

        return user;
    }
}
