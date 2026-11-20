package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.AssignmentRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.AssignmentResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Assignment;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.enums.SubmissionMethod;
import com.college.OnlineCourseManagement.LearningPlatform.exception.BadRequestException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.AssignmentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Locale;
import java.util.Collections;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private static final long DEFAULT_MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> SUPPORTED_FILE_TYPES = Set.of("pdf", "doc", "docx", "jpg", "png", "webp");

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentService enrollmentService;
    private final UserService userService;

    @Transactional
    public AssignmentResponse createAssignment(Long courseId, AssignmentRequest request) {
        Course course = getCourse(courseId);
        User instructor = userService.getAuthenticatedUser();
        assertCanManage(course, instructor);
        assertDueDateInFuture(request);

        Assignment assignment = Assignment.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .maxMarks(request.getMaxMarks() == null ? BigDecimal.valueOf(100) : request.getMaxMarks())
                .allowedSubmissionMethods(normalizeMethods(request.getAllowedSubmissionMethods()))
                .allowedFileTypes(normalizeFileTypes(request.getAllowedFileTypes()))
                .maxFileSize(normalizeMaxFileSize(request.getMaxFileSize()))
                .allowLateSubmissions(request.isAllowLateSubmissions())
                .course(course)
                .createdBy(instructor)
                .build();

        return toResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public AssignmentResponse updateAssignment(Long id, AssignmentRequest request) {
        Assignment assignment = getAssignment(id);
        assertCanManage(assignment.getCourse(), userService.getAuthenticatedUser());
        assignment.setTitle(request.getTitle().trim());
        assignment.setDescription(request.getDescription());
        assignment.setDueDate(request.getDueDate());
        assignment.setMaxMarks(request.getMaxMarks() == null ? BigDecimal.valueOf(100) : request.getMaxMarks());
        assignment.setAllowedSubmissionMethods(normalizeMethods(request.getAllowedSubmissionMethods()));
        assignment.setAllowedFileTypes(normalizeFileTypes(request.getAllowedFileTypes()));
        assignment.setMaxFileSize(normalizeMaxFileSize(request.getMaxFileSize()));
        assignment.setAllowLateSubmissions(request.isAllowLateSubmissions());
        return toResponse(assignment);
    }

    /**
     * Only enforced on creation: an instructor must still be able to extend or correct the
     * deadline of an assignment whose due date has already passed.
     */
    private void assertDueDateInFuture(AssignmentRequest request) {
        if (request.getDueDate() != null && !request.getDueDate().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Due date must be in the future");
        }
    }

    @Transactional
    public void deleteAssignment(Long id) {
        Assignment assignment = getAssignment(id);
        assertCanManage(assignment.getCourse(), userService.getAuthenticatedUser());
        assignmentRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getCourseAssignments(Long courseId) {
        Course course = getCourse(courseId);
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Course not found");
        }
        enrollmentService.assertCanViewCourseContent(course, userService.getAuthenticatedUser());

        return assignmentRepository.findByCourseIdOrderByDueDateAsc(courseId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getManagedAssignments(Long courseId) {
        Course course = getCourse(courseId);
        assertCanManage(course, userService.getAuthenticatedUser());
        return assignmentRepository.findByCourseIdOrderByDueDateAsc(courseId).stream()
                .map(this::toResponse)
                .toList();
    }

    // Public helpers: SubmissionService calls these, so they must own a transaction to
    // safely dereference the lazy course/instructors proxies.
    @Transactional(readOnly = true)
    public Assignment getAssignment(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
    }

    @Transactional(readOnly = true)
    public void assertCanManage(Course course, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() != Role.INSTRUCTOR || course.getInstructors().stream()
                .noneMatch(instructor -> instructor.getId().equals(user.getId()))) {
            throw new ForbiddenException("You are not allowed to manage this course");
        }
    }

    private Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }

    private AssignmentResponse toResponse(Assignment assignment) {
        return AssignmentResponse.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .dueDate(assignment.getDueDate())
                .maxMarks(assignment.getMaxMarks() == null ? BigDecimal.valueOf(100) : assignment.getMaxMarks())
                .allowedSubmissionMethods(assignment.getAllowedSubmissionMethods() == null
                        || assignment.getAllowedSubmissionMethods().isEmpty()
                        ? Set.of(SubmissionMethod.FILE, SubmissionMethod.URL, SubmissionMethod.TEXT)
                        : Set.copyOf(assignment.getAllowedSubmissionMethods()))
                .allowedFileTypes(assignment.getAllowedFileTypes() == null
                        ? SUPPORTED_FILE_TYPES
                        : Set.copyOf(assignment.getAllowedFileTypes()))
                .maxFileSize(assignment.getMaxFileSize() <= 0 ? DEFAULT_MAX_FILE_SIZE : assignment.getMaxFileSize())
                .allowLateSubmissions(assignment.isAllowLateSubmissions())
                .courseId(assignment.getCourse().getId())
                .createdById(assignment.getCreatedBy().getId())
                .createdAt(assignment.getCreatedAt())
                .build();
    }

    private Set<SubmissionMethod> normalizeMethods(Set<SubmissionMethod> methods) {
        if (methods == null || methods.isEmpty()) {
            return Set.of(SubmissionMethod.FILE, SubmissionMethod.URL, SubmissionMethod.TEXT);
        }
        return Set.copyOf(methods);
    }

    private Set<String> normalizeFileTypes(Set<String> fileTypes) {
        if (fileTypes == null || fileTypes.isEmpty()) {
            return SUPPORTED_FILE_TYPES;
        }
        Set<String> normalized = new HashSet<>();
        for (String fileType : fileTypes) {
            String value = fileType == null ? "" : fileType.trim().toLowerCase(Locale.ROOT);
            if (!SUPPORTED_FILE_TYPES.contains(value)) {
                throw new BadRequestException("Unsupported file type: " + fileType);
            }
            normalized.add(value);
        }
        return Set.copyOf(normalized);
    }

    private long normalizeMaxFileSize(Long maxFileSize) {
        return maxFileSize == null ? DEFAULT_MAX_FILE_SIZE : maxFileSize;
    }
}
