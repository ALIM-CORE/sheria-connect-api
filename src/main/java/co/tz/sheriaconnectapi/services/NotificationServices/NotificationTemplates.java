package co.tz.sheriaconnectapi.services.NotificationServices;

import java.util.Map;

public final class NotificationTemplates {
    private NotificationTemplates() {
    }

    public static NotificationTemplate caseStatusChanged(String caseNumber, String status) {
        return template(
                "caseStatusChanged.title",
                "caseStatusChanged.body",
                Map.of("caseNumber", caseNumber, "status", status)
        );
    }

    public static NotificationTemplate matchingRequestCreated() {
        return template(
                "matchingRequestCreated.title",
                "matchingRequestCreated.body",
                Map.of()
        );
    }

    public static NotificationTemplate matchingRequestUpdated(
            String caseNumber,
            String status
    ) {
        return template(
                "matchingRequestUpdated." + status + ".title",
                "matchingRequestUpdated.body",
                Map.of("caseNumber", caseNumber, "status", status)
        );
    }

    public static NotificationTemplate providerVerification(String status) {
        return template(
                "providerVerification.title",
                "providerVerification.body",
                Map.of("status", status)
        );
    }

    public static NotificationTemplate caseMessageFromCitizen(String caseNumber) {
        return template(
                "caseMessageFromCitizen.title",
                "caseMessageFromCitizen.body",
                Map.of("caseNumber", caseNumber)
        );
    }

    public static NotificationTemplate caseMessageFromProvider(String caseNumber) {
        return template(
                "caseMessageFromProvider.title",
                "caseMessageFromProvider.body",
                Map.of("caseNumber", caseNumber)
        );
    }

    public static NotificationTemplate storyModeration(String storyTitle, String status) {
        return template(
                "storyModeration.title",
                "storyModeration." + status + ".body",
                Map.of("storyTitle", storyTitle, "status", status)
        );
    }

    private static NotificationTemplate template(
            String titleKey,
            String bodyKey,
            Map<String, String> params
    ) {
        return new NotificationTemplate(titleKey, bodyKey, params);
    }
}
