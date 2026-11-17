package com.college.OnlineCourseManagement.LearningPlatform.repository;

import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByIdAndStatus(Long id, CourseStatus status);

    @Query("""
            SELECT DISTINCT c FROM Course c
            LEFT JOIN FETCH c.category
            LEFT JOIN FETCH c.instructors
            WHERE c.id = :id
            """)
    Optional<Course> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT c FROM Course c
            LEFT JOIN FETCH c.category
            LEFT JOIN FETCH c.instructors
            WHERE c.status = :status
            ORDER BY c.createdAt DESC
            """)
    List<Course> findByStatusWithDetails(@Param("status") CourseStatus status);

        @Query("""
                        SELECT DISTINCT c FROM Course c
                        JOIN c.instructors instructor
                        LEFT JOIN FETCH c.category
                        WHERE instructor.id = :instructorId
                        ORDER BY c.createdAt DESC
                        """)
        List<Course> findByInstructorIdWithDetails(@Param("instructorId") Long instructorId);

        @Query("""
                SELECT DISTINCT c FROM Course c
                JOIN FETCH c.category
                LEFT JOIN FETCH c.instructors
                WHERE c.category.id = :categoryId
                ORDER BY c.createdAt DESC
                """)
        List<Course> findByCategoryIdWithDetails(@Param("categoryId") Long categoryId);

        boolean existsByInstructorsId(Long instructorId);
}
