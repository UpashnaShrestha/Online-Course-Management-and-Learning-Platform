package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.CourseProgressResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.LessonProgressResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Lesson;
import com.college.OnlineCourseManagement.LearningPlatform.entity.LessonProgress;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.EnrollmentStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CourseRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.EnrollmentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.LessonProgressRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LessonProgressService {

    private final LessonProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseCompletionService courseCompletionService;
    private final UserService userService;

    @Transactional
    public LessonProgressResponse completeLesson(Long lessonId) {
        User student = getStudent();
        Lesson lesson = getLesson(lessonId);
        Long courseId = lesson.getModule().getCourse().getId();
        assertEnrolled(student, courseId);

        LessonProgress progress = progressRepository.findByStudentAndLesson(student.getId(), lessonId)
                .orElseGet(() -> LessonProgress.builder()
                        .student(student)
                        .lesson(lesson)
                        .build());

        if (!progress.isCompleted()) {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
        }

        LessonProgress savedProgress = progressRepository.save(progress);
        courseCompletionService.completeCourseIfEligible(student, lesson.getModule().getCourse());
        return toResponse(savedProgress);
    }

    @Transactional(readOnly = true)
    public CourseProgressResponse getCourseProgress(Long courseId) {
        User student = getStudent();
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        assertEnrolled(student, courseId);

        long totalLessons = progressRepository.countLessonsByCourseId(courseId);
        long completedLessons = progressRepository.countCompletedLessons(student.getId(), courseId);
        BigDecimal percentage = totalLessons == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(completedLessons)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalLessons), 2, RoundingMode.HALF_UP);

        return CourseProgressResponse.builder()
                .courseId(courseId)
                .completedLessons(completedLessons)
                .totalLessons(totalLessons)
                .percentage(percentage)
                .completedLessonIds(progressRepository.findCompletedLessonIds(student.getId(), courseId))
                .build();
    }

    private User getStudent() {
        User student = userService.getAuthenticatedUser();
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can access lesson progress");
        }
        return student;
    }

    private Lesson getLesson(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
    }

    private void assertEnrolled(User student, Long courseId) {
        if (!enrollmentRepository.existsByStudentIdAndCourseIdAndStatusNot(
            student.getId(), courseId, EnrollmentStatus.CANCELLED
        )) {
            throw new ForbiddenException("You must be enrolled in the course");
        }
    }

    private LessonProgressResponse toResponse(LessonProgress progress) {
        return LessonProgressResponse.builder()
                .id(progress.getId())
                .lessonId(progress.getLesson().getId())
                .completed(progress.isCompleted())
                .completedAt(progress.getCompletedAt())
                .build();
    }
}
