package co.tz.sheriaconnectapi.services.NotificationServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.NotificationListResponse;
import co.tz.sheriaconnectapi.model.DTOs.NotificationResponse;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.repositories.UserNotificationRepository;
import co.tz.sheriaconnectapi.security.Access.AuthenticatedUserResolver;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ListNotificationsService implements Query<Authentication, NotificationListResponse> {

    private final UserNotificationRepository userNotificationRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ListNotificationsService(
            UserNotificationRepository userNotificationRepository,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.userNotificationRepository = userNotificationRepository;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @Override
    public ResponseEntity<StandardResponse<NotificationListResponse>> execute(
            Authentication authentication
    ) {
        User user = authenticatedUserResolver.requireUser(authentication);
        AccessContext context = authenticatedUserResolver.requireContext(authentication);
        NotificationListResponse response = new NotificationListResponse(
                userNotificationRepository.countByUserAndContextAndReadAtIsNull(user, context),
                userNotificationRepository.findByUserAndContextOrderByCreatedAtDesc(user, context)
                        .stream()
                        .map(NotificationResponse::new)
                        .toList()
        );

        return ResponseUtil.success(response, "Notifications retrieved", HttpStatus.OK);
    }
}
