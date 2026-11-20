package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.SubmissionRequest;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Assignment;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Submission;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.enums.SubmissionMethod;
import com.college.OnlineCourseManagement.LearningPlatform.enums.SubmissionStatus;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.EnrollmentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceLateSubmissionTest {

    @Mock private SubmissionRepository submissionRepository;
    @Mock private AssignmentService assignmentService;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CourseCompletionService courseCompletionService;
    @Mock private UserService userService;
    @Mock private NotificationService notificationService;
    @Mock private FileStorageService fileStorageService;

    private SubmissionService service;
    private User student;
    private Course course;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        service = new SubmissionService(
                submissionRepository,
                assignmentService,
                enrollmentRepository,
                courseCompletionService,
                userService,
                notificationService,
                fileStorageService
        );
        student = User.builder().id(1L).username("student").role(Role.STUDENT).build();
        course = Course.builder().id(10L).build();
        assignment = Assignment.builder()
                .id(20L)
                .course(course)
                .dueDate(LocalDateTime.now().minusHours(1))
                .allowedSubmissionMethods(Set.of(SubmissionMethod.TEXT))
                .allowLateSubmissions(true)
                .build();
        when(userService.getAuthenticatedUser()).thenReturn(student);
        when(assignmentService.getAssignment(20L)).thenReturn(assignment);
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatusNot(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(10L),
                any()
        )).thenReturn(true);
    }

    @Test
    void allowsLateSubmissionAndMarksItLate() {
        SubmissionRequest request = SubmissionRequest.builder()
                .submissionMethod(SubmissionMethod.TEXT)
                .text("My answer")
                .build();
        when(submissionRepository.countByAssignmentIdAndStudentId(20L, 1L)).thenReturn(0);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.submit(20L, request);

        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.LATE);
        assertThat(response.isLate()).isTrue();
        assertThat(response.getSubmittedAt()).isAfter(assignment.getDueDate());
    }

    @Test
    void rejectsLateSubmissionWhenInstructorDisabledLateWork() {
        assignment.setAllowLateSubmissions(false);
        SubmissionRequest request = SubmissionRequest.builder()
                .submissionMethod(SubmissionMethod.TEXT)
                .text("Too late")
                .build();

        assertThatThrownBy(() -> service.submit(20L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Late submissions are disabled");
    }
}
