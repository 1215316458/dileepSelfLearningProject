package com.ecommerce.notification_service.dto;

import com.ecommerce.notification_service.domain.Notification;
import com.ecommerce.notification_service.domain.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        String id,
        Long userId,
        NotificationType type,
        String title,
        String message,
        boolean read,
        Instant createdAt,
        Long referenceId
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getUserId(), n.getType(),
                n.getTitle(), n.getMessage(), n.isRead(),
                n.getCreatedAt(), n.getReferenceId()
        );
    }
}
