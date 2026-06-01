package co.tz.sheriaconnectapi.model.DTOs;

import java.util.List;

public record NotificationListResponse(
        long unreadCount,
        List<NotificationResponse> notifications
) {
}
