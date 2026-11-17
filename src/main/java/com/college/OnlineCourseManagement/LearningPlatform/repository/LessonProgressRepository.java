package com.college.OnlineCourseManagement.LearningPlatform.repository;

import com.college.OnlineCourseManagement.LearningPlatform.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    boolean existsByStudentId(Long studentId);


    @Query("""
            SELECT progress FROM LessonProgress progress
            WHERE progress.student.id = :studentId AND progress.lesson.id = :lessonId
            """)
    java.util.Optional<LessonProgress> findByStudentAndLesson(
            @Param("studentId") Long studentId,
            @Param("lessonId") Long lessonId
    );

    @Query("""
            SELECT COUNT(lesson) FROM Lesson lesson
            WHERE lesson.module.course.id = :courseId
            """)
    long countLessonsByCourseId(@Param("courseId") Long courseId);

    @Query("""
            SELECT COUNT(progress) FROM LessonProgress progress
            WHERE progress.student.id = :studentId
              AND progress.lesson.module.course.id = :courseId
              AND progress.completed = true
            """)
    long countCompletedLessons(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId
    );

    @Query("""
            SELECT progress.lesson.id FROM LessonProgress progress
            WHERE progress.student.id = :studentId
              AND progress.lesson.module.course.id = :courseId
              AND progress.completed = true
            """)
    java.util.List<Long> findCompletedLessonIds(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId
    );
}
