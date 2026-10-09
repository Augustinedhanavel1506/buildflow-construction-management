package com.buildflow.notification.dto;

import com.buildflow.notification.entity.Notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        String entityType,
        Long entityId,
        Long projectId,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getEntityType(),
                notification.getEntityId(),
                notification.getProjectId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
