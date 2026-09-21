package co.tz.sheriaconnectapi.services.NotificationServices;

import co.tz.sheriaconnectapi.model.DTOs.NotificationResponse;
import co.tz.sheriaconnectapi.model.Entities.UserNotification;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NotificationTemplatesTest {

    @Test
    void producesStableKeysAndRawStatusParameters() {
        NotificationTemplate caseStatus =
                NotificationTemplates.caseStatusChanged("SC-2609-ABC234", "UNDER_REVIEW");
        NotificationTemplate matching =
                NotificationTemplates.matchingRequestUpdated("SC-2609-ABC234", "ACCEPTED");
        NotificationTemplate story =
                NotificationTemplates.storyModeration("My story", "PUBLISHED");

        assertEquals("caseStatusChanged.title", caseStatus.titleKey());
        assertEquals("UNDER_REVIEW", caseStatus.params().get("status"));
        assertEquals("matchingRequestUpdated.ACCEPTED.title", matching.titleKey());
        assertEquals("ACCEPTED", matching.params().get("status"));
        assertEquals("storyModeration.PUBLISHED.body", story.bodyKey());
        assertEquals("My story", story.params().get("storyTitle"));
    }

    @Test
    void coversTheRemainingNotificationFamilies() {
        assertEquals(
                "matchingRequestCreated.title",
                NotificationTemplates.matchingRequestCreated().titleKey()
        );
        assertEquals(
                "providerVerification.body",
                NotificationTemplates.providerVerification("VERIFIED").bodyKey()
        );
        assertEquals(
                "caseMessageFromCitizen.title",
                NotificationTemplates.caseMessageFromCitizen("SC-1").titleKey()
        );
        assertEquals(
                "caseMessageFromProvider.body",
                NotificationTemplates.caseMessageFromProvider("SC-1").bodyKey()
        );
    }

    @Test
    void legacyNotificationKeepsEnglishFallbackWithNullTemplateFields() {
        UserNotification notification = new UserNotification();
        notification.setId(12L);
        notification.setType(NotificationType.CASE_STATUS_CHANGED);
        notification.setContext(AccessContext.CITIZEN);
        notification.setTitle("Case status updated");
        notification.setBody("Case SC-1 is now under review.");
        notification.setCreatedAt(Instant.parse("2026-09-21T10:00:00Z"));

        NotificationResponse response = new NotificationResponse(notification);

        assertEquals("Case status updated", response.title());
        assertEquals("Case SC-1 is now under review.", response.body());
        assertNull(response.titleKey());
        assertNull(response.bodyKey());
        assertNull(response.params());
    }
}
