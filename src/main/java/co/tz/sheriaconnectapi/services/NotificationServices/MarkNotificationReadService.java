package co.tz.sheriaconnectapi.services.NotificationServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.NotificationNotFoundException;
import co.tz.sheriaconnectapi.exceptions.NotificationAccessDeniedException;
import co.tz.sheriaconnectapi.model.DTOs.NotificationResponse;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Entities.UserNotification;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.repositories.UserNotificationRepository;
import co.tz.sheriaconnectapi.security.Access.AuthenticatedUserResolver;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class MarkNotificationReadService
        implements Command<MarkNotificationReadService.Input, NotificationResponse> {

    private final UserNotificationRepository userNotificationRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public MarkNotificationReadService(
            UserNotificationRepository userNotificationRepository,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.userNotificationRepository = userNotificationRepository;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @Override
    public ResponseEntity<StandardResponse<NotificationResponse>> execute(Input input) {
        User user = authenticatedUserResolver.requireUser(input.authentication());
        AccessContext context = authenticatedUserResolver.requireContext(input.authentication());
        UserNotification notification = userNotificationRepository.findById(input.notificationId())
                .orElseThrow(NotificationNotFoundException::new);

        if (!notification.getUser().getId().equals(user.getId())
                || notification.getContext() != context) {
            throw new NotificationAccessDeniedException();
        }

        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }

        return ResponseUtil.success(
                new NotificationResponse(userNotificationRepository.save(notification)),
                "Notification marked as read",
                HttpStatus.OK
        );
    }

    public record Input(Long notificationId, Authentication authentication) {
    }
}
