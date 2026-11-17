package com.college.OnlineCourseManagement.LearningPlatform.entity;

import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_deletion_audits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDeletionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deleted_user_id", nullable = false)
    private Long deletedUserId;

    @Column(name = "deleted_username", nullable = false, length = 50)
    private String deletedUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "deleted_user_role", nullable = false, length = 20)
    private Role deletedUserRole;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;

    @Column(name = "admin_username", nullable = false, length = 50)
    private String adminUsername;

    @Column(name = "deleted_at", nullable = false, updatable = false)
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        deletedAt = LocalDateTime.now();
    }
}
