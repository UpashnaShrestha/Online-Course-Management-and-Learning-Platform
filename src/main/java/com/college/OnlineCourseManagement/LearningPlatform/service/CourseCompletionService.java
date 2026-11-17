package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.entity.Certificate;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Enrollment;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.EnrollmentStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.NotificationType;
import com.college.OnlineCourseManagement.LearningPlatform.repository.AssignmentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CertificateRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.EnrollmentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.LessonProgressRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseCompletionService {

    private final LessonProgressRepository lessonProgressRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CertificateRepository certificateRepository;
    private final NotificationService notificationService;

    @Transactional
    public void completeCourseIfEligible(User student, Course course) {
        long totalLessons = lessonProgressRepository.countLessonsByCourseId(course.getId());
        long completedLessons = lessonProgressRepository.countCompletedLessons(student.getId(), course.getId());
        long totalAssignments = assignmentRepository.countByCourseId(course.getId());
        long gradedAssignments = submissionRepository.countGradedSubmissions(student.getId(), course.getId());

        // A course with no lessons and no assignments has nothing to complete.
        if (totalLessons == 0 && totalAssignments == 0) {
            return;
        }

        boolean lessonsComplete = totalLessons == 0 || completedLessons >= totalLessons;
        boolean assignmentsComplete = totalAssignments == 0 || gradedAssignments >= totalAssignments;

        if (!lessonsComplete || !assignmentsComplete) {
            return;
        }

        // Completion is a side effect of grading/lesson completion. If the student has no
        // active enrollment there is nothing to mark, and throwing here would roll back the
        // caller's already-applied changes.
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseIdAndStatusNot(
                student.getId(), course.getId(), EnrollmentStatus.CANCELLED
            )
                .orElse(null);

        if (enrollment == null) {
            return;
        }

        if (enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollment.setCompletedAt(LocalDateTime.now());
        }

        if (!certificateRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            Certificate certificate = Certificate.builder()
                    .certificateNumber("CERT-" + UUID.randomUUID().toString().replace("-", "").toUpperCase())
                    .student(student)
                    .course(course)
                    .build();
            certificateRepository.save(certificate);

            notificationService.notify(
                    student,
                    "Course completed",
                    "You completed \"" + course.getTitle() + "\". Your certificate is ready to download.",
                    NotificationType.CERTIFICATE,
                    "/student/certificates"
            );
        }
    }
}
