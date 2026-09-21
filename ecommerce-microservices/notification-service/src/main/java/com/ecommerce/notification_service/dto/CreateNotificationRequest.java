package com.ecommerce.notification_service.dto;

import com.ecommerce.notification_service.domain.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNotificationRequest(
        @NotNull Long userId,
        @NotNull NotificationType type,
        @NotBlank String title,
        @NotBlank String message,
        Long referenceId
) {}
