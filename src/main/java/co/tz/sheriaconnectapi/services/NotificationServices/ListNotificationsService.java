package co.tz.sheriaconnectapi.services.NotificationServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.NotificationListResponse;
import co.tz.sheriaconnectapi.model.DTOs.NotificationResponse;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.UserNotificationRepository;
import co.tz.sheriaconnectapi.services.IncidentReportServices.IncidentReportAccessService;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ListNotificationsService implements Query<Authentication, NotificationListResponse> {

    private final UserNotificationRepository userNotificationRepository;
    private final IncidentReportAccessService accessService;

    public ListNotificationsService(
            UserNotificationRepository userNotificationRepository,
            IncidentReportAccessService accessService
    ) {
        this.userNotificationRepository = userNotificationRepository;
        this.accessService = accessService;
    }

    @Override
    public ResponseEntity<StandardResponse<NotificationListResponse>> execute(
            Authentication authentication
    ) {
        User user = accessService.requireAuthenticatedUser(authentication);
        NotificationListResponse response = new NotificationListResponse(
                userNotificationRepository.countByUserAndReadAtIsNull(user),
                userNotificationRepository.findByUserOrderByCreatedAtDesc(user)
                        .stream()
                        .map(NotificationResponse::new)
                        .toList()
        );

        return ResponseUtil.success(response, "Notifications retrieved", HttpStatus.OK);
    }
}
