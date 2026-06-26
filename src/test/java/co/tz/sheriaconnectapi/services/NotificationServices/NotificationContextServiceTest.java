package co.tz.sheriaconnectapi.services.NotificationServices;

import co.tz.sheriaconnectapi.exceptions.NotificationAccessDeniedException;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Entities.UserNotification;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.NotificationType;
import co.tz.sheriaconnectapi.repositories.UserNotificationRepository;
import co.tz.sheriaconnectapi.security.Access.AuthenticatedUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationContextServiceTest {

    @Mock
    private UserNotificationRepository notificationRepository;
    @Mock
    private AuthenticatedUserResolver authenticatedUserResolver;
    @Mock
    private Authentication authentication;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(5L);
        user.setEmail("multi-role@sheriaconnect.co.tz");
    }

    @Test
    void listsOnlyNotificationsForTheActiveContext() {
        UserNotification staffNotification = notification(user, AccessContext.STAFF);
        when(authenticatedUserResolver.requireUser(authentication)).thenReturn(user);
        when(authenticatedUserResolver.requireContext(authentication))
                .thenReturn(AccessContext.STAFF);
        when(notificationRepository.countByUserAndContextAndReadAtIsNull(
                user,
                AccessContext.STAFF
        )).thenReturn(1L);
        when(notificationRepository.findByUserAndContextOrderByCreatedAtDesc(
                user,
                AccessContext.STAFF
        )).thenReturn(List.of(staffNotification));

        var response = new ListNotificationsService(
                notificationRepository,
                authenticatedUserResolver
        ).execute(authentication);

        assertEquals(1, response.getBody().getBody().notifications().size());
        verify(notificationRepository).findByUserAndContextOrderByCreatedAtDesc(
                user,
                AccessContext.STAFF
        );
    }

    @Test
    void cannotMarkANotificationFromAnotherContext() {
        UserNotification citizenNotification = notification(user, AccessContext.CITIZEN);
        when(authenticatedUserResolver.requireUser(authentication)).thenReturn(user);
        when(authenticatedUserResolver.requireContext(authentication))
                .thenReturn(AccessContext.PROVIDER);
        when(notificationRepository.findById(3L))
                .thenReturn(Optional.of(citizenNotification));

        var service = new MarkNotificationReadService(
                notificationRepository,
                authenticatedUserResolver
        );

        assertThrows(
                NotificationAccessDeniedException.class,
                () -> service.execute(new MarkNotificationReadService.Input(3L, authentication))
        );
    }

    @Test
    void dispatchPersistsTheDeclaredContext() {
        NotificationDispatchService service =
                new NotificationDispatchService(notificationRepository);

        service.notify(
                user,
                AccessContext.PROVIDER,
                NotificationType.MATCHING_REQUEST_CREATED,
                "New request",
                "A request is waiting.",
                "CASE_REQUEST",
                "14"
        );

        var captor = org.mockito.ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(AccessContext.PROVIDER, captor.getValue().getContext());
    }

    private UserNotification notification(User owner, AccessContext context) {
        UserNotification notification = new UserNotification();
        notification.setId(3L);
        notification.setUser(owner);
        notification.setContext(context);
        notification.setType(NotificationType.CASE_STATUS_CHANGED);
        notification.setTitle("Update");
        notification.setCreatedAt(Instant.now());
        return notification;
    }
}
