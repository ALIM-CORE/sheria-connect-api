package co.tz.sheriaconnectapi.services.NotificationServices;

import java.util.Map;

public record NotificationTemplate(
        String titleKey,
        String bodyKey,
        Map<String, String> params
) {
    public NotificationTemplate {
        params = params == null || params.isEmpty() ? Map.of() : Map.copyOf(params);
    }
}
