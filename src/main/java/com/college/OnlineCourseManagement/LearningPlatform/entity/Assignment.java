package com.college.OnlineCourseManagement.LearningPlatform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import com.college.OnlineCourseManagement.LearningPlatform.enums.SubmissionMethod;

@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "max_marks", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal maxMarks = BigDecimal.valueOf(100);

    @ElementCollection
    @CollectionTable(name = "assignment_submission_methods", joinColumns = @JoinColumn(name = "assignment_id"))
    @Column(name = "submission_method", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<SubmissionMethod> allowedSubmissionMethods = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "assignment_allowed_file_types", joinColumns = @JoinColumn(name = "assignment_id"))
    @Column(name = "file_type", nullable = false, length = 10)
    @Builder.Default
    private Set<String> allowedFileTypes = new HashSet<>();

    @Column(name = "max_file_size", nullable = false)
    @Builder.Default
    private long maxFileSize = 5 * 1024 * 1024;

    @Column(name = "allow_late_submissions", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean allowLateSubmissions = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
