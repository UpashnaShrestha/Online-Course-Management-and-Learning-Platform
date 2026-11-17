package com.college.OnlineCourseManagement.LearningPlatform.repository;

import com.college.OnlineCourseManagement.LearningPlatform.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    boolean existsByStudentId(Long studentId);

    List<Submission> findByAssignmentIdOrderBySubmittedAtAsc(Long assignmentId);
    List<Submission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);
    List<Submission> findByAssignmentCourseIdIn(Collection<Long> courseIds);
    List<Submission> findByAssignmentCourseIdAndStudentIdOrderBySubmittedAtDesc(Long courseId, Long studentId);

    Optional<Submission> findTopByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(
            Long assignmentId, Long studentId);

    List<Submission> findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(
            Long assignmentId, Long studentId);

    int countByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

        @Query("""
                        SELECT COUNT(DISTINCT submission.assignment.id) FROM Submission submission
                        WHERE submission.student.id = :studentId
                            AND submission.assignment.course.id = :courseId
                            AND submission.status = com.college.OnlineCourseManagement.LearningPlatform.enums.SubmissionStatus.GRADED
                        """)
        long countGradedSubmissions(
                        @Param("studentId") Long studentId,
                        @Param("courseId") Long courseId
        );
}
