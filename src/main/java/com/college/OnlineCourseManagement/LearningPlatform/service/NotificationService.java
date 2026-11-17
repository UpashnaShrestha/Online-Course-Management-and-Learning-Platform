package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.NotificationResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Notification;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.NotificationType;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    /**
     * Creates a notification for a user. This is the only supported way to raise a
     * notification; the UI polls for the results.
     */
    @Transactional
    public NotificationResponse notify(User user, String title, String message,
                                       NotificationType type, String link) {
        if (user == null) {
            return null;
        }

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type == null ? NotificationType.SYSTEM : type)
                .link(link)
                .read(false)
                .build();

        return toResponse(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications() {
        User user = userService.getAuthenticatedUser();
        // Bounded: this endpoint is polled every 15s, so it must not grow without limit.
        return notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(Long id) {
        User user = userService.getAuthenticatedUser();
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You are not allowed to update this notification");
        }

        notification.setRead(true);
        return toResponse(notification);
    }

    @Transactional
    public long markAllAsRead() {
        User user = userService.getAuthenticatedUser();
        return notificationRepository.markAllAsRead(user.getId());
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(notification.isRead())
                .type(notification.getType())
                .link(notification.getLink())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}