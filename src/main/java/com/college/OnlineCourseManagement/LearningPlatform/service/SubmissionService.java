package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ApiResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.GradeSubmissionRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.SubmissionRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.SubmissionResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.*;
import com.college.OnlineCourseManagement.LearningPlatform.enums.EnrollmentStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.NotificationType;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.enums.SubmissionMethod;
import com.college.OnlineCourseManagement.LearningPlatform.enums.SubmissionStatus;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.BadRequestException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.EnrollmentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentService assignmentService;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseCompletionService courseCompletionService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;

    @Transactional
    public SubmissionResponse submit(Long assignmentId, SubmissionRequest request) {
        User student = userService.getAuthenticatedUser();
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can submit assignments");
        }

        Assignment assignment = assignmentService.getAssignment(assignmentId);
        if (!enrollmentRepository.existsByStudentIdAndCourseIdAndStatusNot(
                student.getId(), assignment.getCourse().getId(), EnrollmentStatus.CANCELLED
        )) {
            throw new ForbiddenException("You must be enrolled in the course to submit this assignment");
        }

        LocalDateTime submittedAt = LocalDateTime.now();
        boolean late = assignment.getDueDate() != null && submittedAt.isAfter(assignment.getDueDate());
        if (late && !assignment.isAllowLateSubmissions()) {
            throw new ForbiddenException(
                    "Late submissions are disabled for this assignment. "
                    + "The deadline passed at " + assignment.getDueDate() + "."
            );
        }

        SubmissionMethod method = resolveMethod(request);
        Set<SubmissionMethod> allowedMethods = assignment.getAllowedSubmissionMethods();
        if (allowedMethods != null && !allowedMethods.isEmpty() && !allowedMethods.contains(method)) {
            throw new ForbiddenException("This submission method is not allowed for the assignment");
        }

        String filePath = "";
        String fileName = null;
        String fileType = null;
        String textContent = null;

        if (method == SubmissionMethod.FILE) {
            if (request.getFile() == null || request.getFile().isEmpty()) {
                throw new BadRequestException("An uploaded file is required");
            }
            Set<String> allowedTypes = assignment.getAllowedFileTypes();
            String storedFileName = fileStorageService.storeFile(
                    request.getFile(), assignment.getMaxFileSize(), allowedTypes);
            filePath = storedFileName;
            fileName = request.getFile().getOriginalFilename();
            fileType = extensionOf(fileName);
        } else if (method == SubmissionMethod.URL) {
            if (request.getUrl() == null || request.getUrl().isBlank()) {
                throw new BadRequestException("A submission URL is required");
            }
            filePath = request.getUrl().trim();
            fileType = "url";
        } else {
            if (request.getText() == null || request.getText().isBlank()) {
                throw new BadRequestException("A text submission is required");
            }
            textContent = request.getText().trim();
            fileType = "text";
        }

        int attemptNumber = submissionRepository.countByAssignmentIdAndStudentId(
                assignmentId, student.getId()) + 1;
        Submission submission = Submission.builder()
                .assignment(assignment)
                .student(student)
                .filePath(filePath)
                .fileName(fileName)
                .fileType(fileType)
                .submissionMethod(method)
                .textContent(textContent)
                .attemptNumber(attemptNumber)
                .status(late ? SubmissionStatus.LATE : SubmissionStatus.SUBMITTED)
                .submittedAt(submittedAt)
                .late(late)
                .build();

        return toResponse(submissionRepository.save(submission));
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissions(Long assignmentId) {
        Assignment assignment = assignmentService.getAssignment(assignmentId);
        User instructor = userService.getAuthenticatedUser();
        assignmentService.assertCanManage(assignment.getCourse(), instructor);

        Map<Long, Submission> latestByStudent = submissionRepository
                .findByAssignmentIdOrderBySubmittedAtAsc(assignmentId).stream()
                .collect(Collectors.toMap(
                        submission -> submission.getStudent().getId(),
                        Function.identity(),
                        (first, latest) -> latest
                ));

        return enrollmentRepository.findByCourseIdAndStatusNot(
                        assignment.getCourse().getId(), EnrollmentStatus.CANCELLED
                ).stream()
                .map(enrollment -> {
                    Submission latest = latestByStudent.get(enrollment.getStudent().getId());
                    return latest == null
                            ? notSubmittedResponse(assignment, enrollment.getStudent())
                            : toResponse(latest);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getStudentSubmissions() {
        User student = userService.getAuthenticatedUser();
        return submissionRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SubmissionResponse grade(Long submissionId, GradeSubmissionRequest request) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
        assignmentService.assertCanManage(
                submission.getAssignment().getCourse(),
                userService.getAuthenticatedUser()
        );

        if (submission.getStatus() == SubmissionStatus.NOT_SUBMITTED) {
            throw new BadRequestException("A submission is required before grading");
        }
        BigDecimal maxMarks = submission.getAssignment().getMaxMarks() == null
                ? java.math.BigDecimal.valueOf(100)
                : submission.getAssignment().getMaxMarks();
        if (request.getGrade().compareTo(maxMarks) > 0) {
            throw new BadRequestException("Grade must not exceed the assignment maximum marks");
        }

        submission.setGrade(request.getGrade());
        submission.setFeedback(request.getFeedback());
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedAt(LocalDateTime.now());

        String assignmentTitle = submission.getAssignment().getTitle();
        notificationService.notify(
                submission.getStudent(),
                "Assignment graded",
                "\"" + assignmentTitle + "\" has been graded: "
                        + request.getGrade() + "/" + maxMarks + "."
                        + (request.getFeedback() == null || request.getFeedback().isBlank()
                        ? "" : " " + request.getFeedback()),
                NotificationType.GRADE,
                "/student/courses/" + submission.getAssignment().getCourse().getId() + "/learn"
        );

        courseCompletionService.completeCourseIfEligible(
            submission.getStudent(),
            submission.getAssignment().getCourse()
        );
        return toResponse(submission);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getSubmissionFile(Long submissionId, boolean download) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
        User currentUser = userService.getAuthenticatedUser();
        if (currentUser.getRole() == Role.STUDENT
                && !submission.getStudent().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not allowed to access this submission");
        }
        if (currentUser.getRole() != Role.STUDENT) {
            assignmentService.assertCanManage(submission.getAssignment().getCourse(), currentUser);
        }
        if (submission.getSubmissionMethod() != SubmissionMethod.FILE) {
            throw new BadRequestException("This submission does not contain an uploaded file");
        }
        return fileStorageService.serveFile(submission.getFilePath(), download, submission.getFileName());
    }

    private SubmissionMethod resolveMethod(SubmissionRequest request) {
        if (request.getSubmissionMethod() != null) {
            return request.getSubmissionMethod();
        }
        if (request.getFile() != null && !request.getFile().isEmpty()) return SubmissionMethod.FILE;
        if (request.getUrl() != null && !request.getUrl().isBlank()) return SubmissionMethod.URL;
        if (request.getText() != null && !request.getText().isBlank()) return SubmissionMethod.TEXT;
        throw new BadRequestException("Choose a submission method");
    }

    private String extensionOf(String filename) {
        if (filename == null || filename.lastIndexOf('.') < 0) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private SubmissionResponse notSubmittedResponse(Assignment assignment, User student) {
        return SubmissionResponse.builder()
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .studentName((student.getFirstName() + " " + student.getLastName()).trim())
                .studentUsername(student.getUsername())
                .status(SubmissionStatus.NOT_SUBMITTED)
                .attemptNumber(0)
                .build();
    }

    private SubmissionResponse toResponse(Submission submission) {
        return SubmissionResponse.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .studentId(submission.getStudent().getId())
                .studentName((submission.getStudent().getFirstName() + " "
                        + submission.getStudent().getLastName()).trim())
                .studentUsername(submission.getStudent().getUsername())
                .filePath(submission.getFilePath())
                .fileName(submission.getFileName())
                .fileType(submission.getFileType())
                .submissionMethod(submission.getSubmissionMethod())
                .textContent(submission.getTextContent())
                .attemptNumber(submission.getAttemptNumber())
                .submittedAt(submission.getSubmittedAt())
                .status(submission.getStatus())
                .grade(submission.getGrade())
                .feedback(submission.getFeedback())
                .gradedAt(submission.getGradedAt())
                .late(submission.isLate())
                .build();
    }
}