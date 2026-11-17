package com.college.OnlineCourseManagement.LearningPlatform.dto;

import com.college.OnlineCourseManagement.LearningPlatform.enums.NotificationType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class NotificationResponse {
    Long id;
    String title;
    String message;
    boolean read;
    NotificationType type;
    /** In-app route the UI should open when this notification is clicked. */
    String link;
    LocalDateTime createdAt;
}