package com.college.OnlineCourseManagement.LearningPlatform.repository;

import com.college.OnlineCourseManagement.LearningPlatform.entity.Enrollment;
import com.college.OnlineCourseManagement.LearningPlatform.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

        boolean existsByStudentIdAndCourseIdAndStatusNot(
            Long studentId,
            Long courseId,
            EnrollmentStatus status
        );

        Optional<Enrollment> findByStudentIdAndCourseIdAndStatusNot(
            Long studentId,
            Long courseId,
            EnrollmentStatus status
        );

    List<Enrollment> findByStudentIdOrderByEnrolledAtDesc(Long studentId);

    List<Enrollment> findByCourseIdAndStatusNot(Long courseId, EnrollmentStatus status);

    long countByCourseIdAndStatusNot(Long courseId, EnrollmentStatus status);

    boolean existsByStudentId(Long studentId);
}
