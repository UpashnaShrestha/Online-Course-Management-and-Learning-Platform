package com.college.OnlineCourseManagement.LearningPlatform.repository;

import com.college.OnlineCourseManagement.LearningPlatform.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByUserId(Long userId);

    /** Bounded list: this endpoint is polled, so it must not grow without limit. */
    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    /** Bulk read so "mark all as read" stays a single statement instead of N updates. */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") Long userId);
}