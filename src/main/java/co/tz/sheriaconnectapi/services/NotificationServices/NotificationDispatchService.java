package co.tz.sheriaconnectapi.services.NotificationServices;

import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Entities.UserNotification;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.NotificationType;
import co.tz.sheriaconnectapi.repositories.UserNotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationDispatchService {

    private final UserNotificationRepository userNotificationRepository;

    public NotificationDispatchService(UserNotificationRepository userNotificationRepository) {
        this.userNotificationRepository = userNotificationRepository;
    }

    public void notify(
            User user,
            AccessContext context,
            NotificationType type,
            String title,
            String body,
            String linkType,
            String linkTarget
    ) {
        if (user == null) {
            return;
        }

        UserNotification notification = new UserNotification();
        notification.setUser(user);
        notification.setContext(context);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setLinkType(linkType);
        notification.setLinkTarget(linkTarget);
        userNotificationRepository.save(notification);
    }
}
