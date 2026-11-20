package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.repository.AssignmentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CertificateRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.EnrollmentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.LessonProgressRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.LessonRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class InstructorDashboardProgressTest {

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private CertificateRepository certificateRepository;
    @Mock private CourseService courseService;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private UserService userService;

    private InstructorDashboardService service;

    @BeforeEach
    void setUp() {
        service = new InstructorDashboardService(
                assignmentRepository,
                certificateRepository,
                courseService,
                enrollmentRepository,
                lessonProgressRepository,
                lessonRepository,
                submissionRepository,
                userService
        );
    }

    @Test
    @DisplayName("averages already-calculated 0-100 progress values once")
    void averageProgressDoesNotMultiplyAgain() {
        BigDecimal average = (BigDecimal) ReflectionTestUtils.invokeMethod(
                service,
                "averageProgress",
                List.of(new BigDecimal("25.00"), new BigDecimal("50.00"), new BigDecimal("75.00"))
        );

        assertThat(average).isEqualByComparingTo("50.00");
        assertThat(average).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("returns zero when no student progress values exist")
    void averageProgressWithNoStudentsReturnsZero() {
        BigDecimal average = (BigDecimal) ReflectionTestUtils.invokeMethod(
                service,
                "averageProgress",
                List.of()
        );

        assertThat(average).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
