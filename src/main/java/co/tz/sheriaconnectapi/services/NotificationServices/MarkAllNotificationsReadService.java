package co.tz.sheriaconnectapi.services.NotificationServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Entities.UserNotification;
import co.tz.sheriaconnectapi.repositories.UserNotificationRepository;
import co.tz.sheriaconnectapi.services.IncidentReportServices.IncidentReportAccessService;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class MarkAllNotificationsReadService implements Command<Authentication, Void> {

    private final UserNotificationRepository userNotificationRepository;
    private final IncidentReportAccessService accessService;

    public MarkAllNotificationsReadService(
            UserNotificationRepository userNotificationRepository,
            IncidentReportAccessService accessService
    ) {
        this.userNotificationRepository = userNotificationRepository;
        this.accessService = accessService;
    }

    @Override
    public ResponseEntity<StandardResponse<Void>> execute(Authentication authentication) {
        User user = accessService.requireAuthenticatedUser(authentication);
        Instant now = Instant.now();
        for (UserNotification notification : userNotificationRepository.findByUserOrderByCreatedAtDesc(user)) {
            if (notification.getReadAt() == null) {
                notification.setReadAt(now);
                userNotificationRepository.save(notification);
            }
        }

        return ResponseUtil.success(null, "Notifications marked as read", HttpStatus.OK);
    }
}
