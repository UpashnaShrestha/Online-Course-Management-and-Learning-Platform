package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.NotificationResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Notification;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.NotificationType;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserService userService;

    private NotificationService notificationService;

    private User user;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, userService);
        user = User.builder().id(1L).username("student").role(Role.STUDENT).build();
    }

    @Test
    @DisplayName("notify() persists an unread notification carrying a type and link")
    void notifyPersistsNotification() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        NotificationResponse response = notificationService.notify(
                user, "Payment successful", "We received Rs.500", NotificationType.PAYMENT, "/payment/success/1");

        assertThat(response.getTitle()).isEqualTo("Payment successful");
        assertThat(response.getType()).isEqualTo(NotificationType.PAYMENT);
        assertThat(response.getLink()).isEqualTo("/payment/success/1");
        assertThat(response.isRead()).isFalse();

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("notify() defaults a null type to SYSTEM and tolerates a null user")
    void notifyDefaultsAndGuards() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(notificationService.notify(null, "t", "m", NotificationType.SYSTEM, null)).isNull();
        assertThat(notificationService.notify(user, "t", "m", null, null).getType())
                .isEqualTo(NotificationType.SYSTEM);
    }

    @Test
    @DisplayName("markAllAsRead bulk-marks only the signed-in user's notifications")
    void markAllAsReadIsScoped() {
        when(userService.getAuthenticatedUser()).thenReturn(user);
        when(notificationRepository.markAllAsRead(1L)).thenReturn(4);

        assertThat(notificationService.markAllAsRead()).isEqualTo(4L);
        verify(notificationRepository).markAllAsRead(1L);
    }

    @Test
    @DisplayName("markAsRead rejects a notification belonging to someone else")
    void markAsReadRejectsForeignNotification() {
        when(userService.getAuthenticatedUser()).thenReturn(user);
        Notification other = Notification.builder().id(5L).user(User.builder().id(99L).build()).build();
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> notificationService.markAsRead(5L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("markAsRead flips the flag for the owner")
    void markAsReadMarksOwnedNotification() {
        when(userService.getAuthenticatedUser()).thenReturn(user);
        Notification owned = Notification.builder().id(5L).user(user).title("t").message("m").build();
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(owned));

        assertThat(notificationService.markAsRead(5L).isRead()).isTrue();
    }

    @Test
    @DisplayName("markAsRead 404s for an unknown id")
    void markAsReadUnknownId() {
        when(userService.getAuthenticatedUser()).thenReturn(user);
        when(notificationRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getMyNotifications maps the user's list only")
    void getMyNotificationsIsScoped() {
        when(userService.getAuthenticatedUser()).thenReturn(user);
        when(notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(
                Notification.builder().id(1L).user(user).title("A").message("m").type(NotificationType.GRADE).build(),
                Notification.builder().id(2L).user(user).title("B").message("m").type(NotificationType.ENROLLMENT).build()));

        List<NotificationResponse> result = notificationService.getMyNotifications();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(NotificationResponse::getTitle).containsExactly("A", "B");
    }
}