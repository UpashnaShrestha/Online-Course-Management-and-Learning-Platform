package com.college.OnlineCourseManagement.LearningPlatform.repository;

import com.college.OnlineCourseManagement.LearningPlatform.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByCourseIdOrderByDueDateAsc(Long courseId);
    List<Assignment> findByCourseIdInOrderByDueDateAsc(List<Long> courseIds);

    long countByCourseId(Long courseId);

    boolean existsByCreatedById(Long userId);
}
