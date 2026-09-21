package com.ecommerce.notification_service.service;

import com.ecommerce.notification_service.domain.Notification;
import com.ecommerce.notification_service.domain.NotificationType;
import com.ecommerce.notification_service.dto.CreateNotificationRequest;
import com.ecommerce.notification_service.dto.NotificationResponse;
import com.ecommerce.notification_service.exception.NotificationNotFoundException;
import com.ecommerce.notification_service.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * No @Transactional needed for single-document operations — MongoDB guarantees
 * atomicity at the document level.  Multi-document transactions (MongoDB 4.0+)
 * would require explicit session management and are only used when you truly
 * need ACID across multiple collections.
 */
@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public NotificationResponse create(CreateNotificationRequest req) {
        Notification notification = new Notification(
                req.userId(), req.type(), req.title(), req.message(), req.referenceId()
        );
        return NotificationResponse.from(repository.save(notification));
    }

    /**
     * Convenience factory — called internally when an order event arrives (Day 25).
     * Keeps the controller thin and the event consumer even thinner.
     */
    public NotificationResponse createOrderNotification(Long userId, Long orderId, NotificationType type) {
        String title = switch (type) {
            case ORDER_PLACED    -> "Order Placed";
            case ORDER_CONFIRMED -> "Order Confirmed";
            case ORDER_SHIPPED   -> "Order Shipped";
            case ORDER_DELIVERED -> "Order Delivered";
            case ORDER_CANCELLED -> "Order Cancelled";
            default              -> "Order Update";
        };
        String message = "Your order #" + orderId + " status: " + type.name().replace("_", " ");
        Notification n = new Notification(userId, type, title, message, orderId);
        return NotificationResponse.from(repository.save(n));
    }

    public Page<NotificationResponse> getByUser(Long userId, Pageable pageable) {
        return repository.findByUserId(userId, pageable).map(NotificationResponse::from);
    }

    public List<NotificationResponse> getUnread(Long userId) {
        return repository.findByUserIdAndReadFalse(userId)
                .stream().map(NotificationResponse::from).toList();
    }

    public long countUnread(Long userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    public NotificationResponse markRead(String id) {
        Notification n = repository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        n.markRead();
        return NotificationResponse.from(repository.save(n));
    }

    public void markAllRead(Long userId) {
        List<Notification> unread = repository.findByUserIdAndReadFalse(userId);
        unread.forEach(Notification::markRead);
        repository.saveAll(unread);
    }

    public void delete(String id) {
        if (!repository.existsById(id)) throw new NotificationNotFoundException(id);
        repository.deleteById(id);
    }

    public void deleteAllForUser(Long userId) {
        repository.deleteByUserId(userId);
    }
}
