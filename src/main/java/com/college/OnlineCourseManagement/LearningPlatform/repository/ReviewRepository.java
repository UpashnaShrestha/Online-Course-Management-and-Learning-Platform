package com.college.OnlineCourseManagement.LearningPlatform.repository;

import com.college.OnlineCourseManagement.LearningPlatform.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByStudentId(Long studentId);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    List<Review> findByCourseIdOrderByCreatedAtDesc(Long courseId);
}
