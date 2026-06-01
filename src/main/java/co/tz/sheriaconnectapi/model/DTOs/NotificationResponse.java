package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.UserNotification;
import co.tz.sheriaconnectapi.model.Enums.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String body,
        String linkType,
        String linkTarget,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
    public NotificationResponse(UserNotification notification) {
        this(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getLinkType(),
                notification.getLinkTarget(),
                notification.getReadAt() != null,
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
