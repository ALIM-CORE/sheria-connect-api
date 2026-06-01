package co.tz.sheriaconnectapi.controllers;

import co.tz.sheriaconnectapi.model.DTOs.NotificationListResponse;
import co.tz.sheriaconnectapi.model.DTOs.NotificationResponse;
import co.tz.sheriaconnectapi.services.NotificationServices.ListNotificationsService;
import co.tz.sheriaconnectapi.services.NotificationServices.MarkAllNotificationsReadService;
import co.tz.sheriaconnectapi.services.NotificationServices.MarkNotificationReadService;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final ListNotificationsService listNotificationsService;
    private final MarkNotificationReadService markNotificationReadService;
    private final MarkAllNotificationsReadService markAllNotificationsReadService;

    public NotificationController(
            ListNotificationsService listNotificationsService,
            MarkNotificationReadService markNotificationReadService,
            MarkAllNotificationsReadService markAllNotificationsReadService
    ) {
        this.listNotificationsService = listNotificationsService;
        this.markNotificationReadService = markNotificationReadService;
        this.markAllNotificationsReadService = markAllNotificationsReadService;
    }

    @GetMapping
    public ResponseEntity<StandardResponse<NotificationListResponse>> list(
            Authentication authentication
    ) {
        return listNotificationsService.execute(authentication);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<StandardResponse<NotificationResponse>> markRead(
            @PathVariable Long notificationId,
            Authentication authentication
    ) {
        return markNotificationReadService.execute(
                new MarkNotificationReadService.Input(notificationId, authentication)
        );
    }

    @PatchMapping("/read-all")
    public ResponseEntity<StandardResponse<Void>> markAllRead(
            Authentication authentication
    ) {
        return markAllNotificationsReadService.execute(authentication);
    }
}
