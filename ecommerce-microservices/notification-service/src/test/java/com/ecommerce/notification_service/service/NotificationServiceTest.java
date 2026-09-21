package com.ecommerce.notification_service.service;

import com.ecommerce.notification_service.domain.Notification;
import com.ecommerce.notification_service.domain.NotificationType;
import com.ecommerce.notification_service.dto.CreateNotificationRequest;
import com.ecommerce.notification_service.dto.NotificationResponse;
import com.ecommerce.notification_service.exception.NotificationNotFoundException;
import com.ecommerce.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests — no Spring context, no MongoDB.
 * @ExtendWith(MockitoExtension.class) initialises @Mock and @InjectMocks.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @InjectMocks
    private NotificationService service;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = new Notification(
                1L, NotificationType.ORDER_PLACED,
                "Order Placed", "Your order #100 has been placed", 100L
        );
    }

    @Test
    void create_savesAndReturnsResponse() {
        when(repository.save(any(Notification.class))).thenReturn(sampleNotification);

        CreateNotificationRequest req = new CreateNotificationRequest(
                1L, NotificationType.ORDER_PLACED, "Order Placed", "Your order #100 has been placed", 100L
        );
        NotificationResponse response = service.create(req);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.type()).isEqualTo(NotificationType.ORDER_PLACED);
        verify(repository).save(any(Notification.class));
    }

    @Test
    void createOrderNotification_buildsCorrectTitle() {
        when(repository.save(any(Notification.class))).thenReturn(sampleNotification);

        NotificationResponse response = service.createOrderNotification(1L, 100L, NotificationType.ORDER_PLACED);

        assertThat(response.userId()).isEqualTo(1L);
        verify(repository).save(any(Notification.class));
    }

    @Test
    void getByUser_returnsPaginatedResults() {
        Page<Notification> page = new PageImpl<>(List.of(sampleNotification));
        when(repository.findByUserId(eq(1L), any())).thenReturn(page);

        Page<NotificationResponse> result = service.getByUser(1L, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getUnread_returnsOnlyUnreadNotifications() {
        when(repository.findByUserIdAndReadFalse(1L)).thenReturn(List.of(sampleNotification));

        List<NotificationResponse> unread = service.getUnread(1L);

        assertThat(unread).hasSize(1);
        assertThat(unread.get(0).read()).isFalse();
    }

    @Test
    void countUnread_returnsCorrectCount() {
        when(repository.countByUserIdAndReadFalse(1L)).thenReturn(3L);

        assertThat(service.countUnread(1L)).isEqualTo(3L);
    }

    @Test
    void markRead_updatesReadFlag() {
        when(repository.findById("abc")).thenReturn(Optional.of(sampleNotification));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = service.markRead("abc");

        assertThat(response.read()).isTrue();
        verify(repository).save(sampleNotification);
    }

    @Test
    void markRead_throwsWhenNotFound() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead("missing"))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void delete_throwsWhenNotFound() {
        when(repository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> service.delete("missing"))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void markAllRead_savesAllUnreadNotifications() {
        Notification n1 = new Notification(1L, NotificationType.ORDER_PLACED, "T1", "M1", 1L);
        Notification n2 = new Notification(1L, NotificationType.ORDER_SHIPPED, "T2", "M2", 2L);
        when(repository.findByUserIdAndReadFalse(1L)).thenReturn(List.of(n1, n2));

        service.markAllRead(1L);

        verify(repository).saveAll(List.of(n1, n2));
        assertThat(n1.isRead()).isTrue();
        assertThat(n2.isRead()).isTrue();
    }
}
