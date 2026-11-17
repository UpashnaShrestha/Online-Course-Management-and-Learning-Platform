package com.college.OnlineCourseManagement.LearningPlatform.repository;

import com.college.OnlineCourseManagement.LearningPlatform.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    boolean existsByStudentId(Long studentId);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    List<Certificate> findByStudentIdOrderByIssuedAtDesc(Long studentId);

    Optional<Certificate> findByIdAndStudentId(Long id, Long studentId);

    List<Certificate> findByStudentId(Long studentId);
}
