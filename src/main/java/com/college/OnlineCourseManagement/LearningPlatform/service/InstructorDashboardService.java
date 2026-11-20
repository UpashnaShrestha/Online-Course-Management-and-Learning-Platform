package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.InstructorCourseSummaryResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.InstructorDashboardResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.InstructorStudentCourseResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.InstructorStudentDetailResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.InstructorStudentResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.SubmissionResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Enrollment;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.EnrollmentStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.enums.SubmissionStatus;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.AssignmentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CertificateRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.EnrollmentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.LessonProgressRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.LessonRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.SubmissionRepository;
import com.college.OnlineCourseManagement.LearningPlatform.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstructorDashboardService {

    private final AssignmentRepository assignmentRepository;
    private final CertificateRepository certificateRepository;
    private final CourseService courseService;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;
    private final SubmissionRepository submissionRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public InstructorDashboardResponse getDashboard() {
        requireInstructor();
        List<Course> courses = getOwnedCourses();
        List<InstructorCourseSummaryResponse> summaries = courses.stream()
                .map(this::toCourseSummary)
                .toList();
        long totalEnrollments = summaries.stream().mapToLong(InstructorCourseSummaryResponse::getEnrolledStudents).sum();
        List<BigDecimal> progressValues = new java.util.ArrayList<>();
        long pendingSubmissions = 0;
        for (Course course : courses) {
            for (Enrollment enrollment : activeEnrollments(course)) {
                progressValues.add(progress(course, enrollment.getStudent().getId()));
            }
            pendingSubmissions += pendingSubmissions(course);
        }

        return InstructorDashboardResponse.builder()
                .courses(summaries)
                .totalCourses(courses.size())
                .totalEnrollments(totalEnrollments)
                .averageProgress(averageProgress(progressValues))
                .pendingSubmissions(pendingSubmissions)
                .build();
    }

    @Transactional(readOnly = true)
    public List<InstructorStudentResponse> getStudents(Long courseId) {
        Course course = courseService.getInstructorCourse(courseId);
        return activeEnrollments(course).stream()
                .map(enrollment -> toStudentSummary(course, enrollment))
                .toList();
    }

    @Transactional(readOnly = true)
    public InstructorStudentDetailResponse getStudent(Long courseId, Long studentId) {
        courseService.getInstructorCourse(courseId);
        User student = activeEnrollments(courseService.getInstructorCourse(courseId)).stream()
                .map(Enrollment::getStudent)
                .filter(candidate -> candidate.getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("This student is not enrolled in the course"));

        List<InstructorStudentCourseResponse> courses = getOwnedCourses().stream()
                .flatMap(course -> activeEnrollments(course).stream()
                        .filter(enrollment -> enrollment.getStudent().getId().equals(studentId))
                        .map(enrollment -> toStudentCourse(course, enrollment)))
                .toList();

        return InstructorStudentDetailResponse.builder()
                .studentId(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .username(student.getUsername())
                .phoneNumber(student.getPhoneNumber())
                .bio(student.getBio())
                .courses(courses)
                .build();
    }

    private List<Course> getOwnedCourses() {
        User instructor = requireInstructor();
        return courseService.getInstructorCourses().stream()
                .map(response -> courseService.getInstructorCourse(response.getId()))
                .toList();
    }

    private User requireInstructor() {
        User user = userService.getAuthenticatedUser();
        if (user.getRole() != Role.INSTRUCTOR) {
            throw new ForbiddenException("Only instructors can access this dashboard");
        }
        return user;
    }

    private InstructorCourseSummaryResponse toCourseSummary(Course course) {
        List<Enrollment> enrollments = activeEnrollments(course);
        long completed = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED).count();
        int totalLessons = (int) lessonRepository.countByModuleCourseId(course.getId());
        List<BigDecimal> progressValues = enrollments.stream()
                .map(e -> progress(course, e.getStudent().getId()))
                .toList();
        return InstructorCourseSummaryResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .thumbnail(course.getThumbnail())
                .price(course.getPrice())
                .status(course.getStatus())
                .createdAt(course.getCreatedAt())
                .enrolledStudents(enrollments.size())
                .completedStudents(completed)
                .totalLessons(totalLessons)
                .totalAssignments(assignmentRepository.countByCourseId(course.getId()))
                .averageProgress(averageProgress(progressValues))
                .build();
    }

    private InstructorStudentResponse toStudentSummary(Course course, Enrollment enrollment) {
        User student = enrollment.getStudent();
        List<SubmissionResponse> submissions = latestSubmissions(course, student.getId()).stream()
                .map(this::toSubmissionResponse)
                .toList();
        long graded = submissions.stream().filter(s -> s.getStatus() == SubmissionStatus.GRADED).count();
        long submitted = submissions.stream().filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED).count();
        long totalAssignments = assignmentRepository.countByCourseId(course.getId());
        return InstructorStudentResponse.builder()
                .studentId(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .username(student.getUsername())
                .enrolledAt(enrollment.getEnrolledAt())
                .progress(progress(course, student.getId()))
                .completedLessons(lessonProgressRepository.countCompletedLessons(student.getId(), course.getId()))
                .totalLessons(lessonRepository.countByModuleCourseId(course.getId()))
                .submittedAssignments(submitted)
                .notSubmittedAssignments(Math.max(0, totalAssignments - submitted - graded))
                .gradedAssignments(graded)
                .courseStatus(enrollment.getStatus())
                .completedAt(enrollment.getCompletedAt())
                .build();
    }

    private InstructorStudentCourseResponse toStudentCourse(Course course, Enrollment enrollment) {
        return InstructorStudentCourseResponse.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .progress(progress(course, enrollment.getStudent().getId()))
                .completedLessons(lessonProgressRepository.countCompletedLessons(enrollment.getStudent().getId(), course.getId()))
                .totalLessons(lessonRepository.countByModuleCourseId(course.getId()))
                .status(enrollment.getStatus())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .submissions(submissions(course, enrollment.getStudent().getId()))
                .certificateIssued(certificateRepository.existsByStudentIdAndCourseId(enrollment.getStudent().getId(), course.getId()))
                .build();
    }

    private List<SubmissionResponse> submissions(Course course, Long studentId) {
        return submissionRepository.findByAssignmentCourseIdAndStudentIdOrderBySubmittedAtDesc(course.getId(), studentId)
                .stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    private SubmissionResponse toSubmissionResponse(com.college.OnlineCourseManagement.LearningPlatform.entity.Submission submission) {
        return SubmissionResponse.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .studentId(submission.getStudent().getId())
                .studentName((submission.getStudent().getFirstName() + " " + submission.getStudent().getLastName()).trim())
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
                .build();
    }

    private List<Enrollment> activeEnrollments(Course course) {
        return enrollmentRepository.findByCourseIdAndStatusNot(course.getId(), EnrollmentStatus.CANCELLED);
    }

    private long pendingSubmissions(Course course) {
        return latestSubmissionsForCourse(course).stream()
                .filter(submission -> submission.getStatus() == SubmissionStatus.SUBMITTED)
                .count();
    }

    private List<com.college.OnlineCourseManagement.LearningPlatform.entity.Submission> submissionsForCourse(Course course) {
        return assignmentRepository.findByCourseIdOrderByDueDateAsc(course.getId()).stream()
                .flatMap(assignment -> submissionRepository.findByAssignmentIdOrderBySubmittedAtAsc(assignment.getId()).stream())
                .toList();
    }

    private List<com.college.OnlineCourseManagement.LearningPlatform.entity.Submission> latestSubmissionsForCourse(Course course) {
        return submissionsForCourse(course).stream()
                .collect(Collectors.toMap(
                        submission -> submission.getAssignment().getId() + ":" + submission.getStudent().getId(),
                        Function.identity(),
                        (first, latest) -> latest.getAttemptNumber() >= first.getAttemptNumber() ? latest : first
                ))
                .values()
                .stream()
                .toList();
    }

    private List<com.college.OnlineCourseManagement.LearningPlatform.entity.Submission> latestSubmissions(
            Course course,
            Long studentId
    ) {
        return latestSubmissionsForCourse(course).stream()
                .filter(submission -> submission.getStudent().getId().equals(studentId))
                .toList();
    }

    private BigDecimal progress(Course course, Long studentId) {
        long total = lessonRepository.countByModuleCourseId(course.getId());
        long completed = lessonProgressRepository.countCompletedLessons(studentId, course.getId());
        return percentage(completed, total);
    }

    private BigDecimal percentage(long value, long total) {
        return total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(value * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageProgress(List<BigDecimal> progressValues) {
        if (progressValues.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return progressValues.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(progressValues.size()), 2, RoundingMode.HALF_UP);
    }
}
