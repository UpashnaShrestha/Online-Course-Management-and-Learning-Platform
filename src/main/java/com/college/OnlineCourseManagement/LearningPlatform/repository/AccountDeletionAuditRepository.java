package com.college.OnlineCourseManagement.LearningPlatform.repository;

import com.college.OnlineCourseManagement.LearningPlatform.entity.AccountDeletionAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountDeletionAuditRepository extends JpaRepository<AccountDeletionAudit, Long> {
}
