package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Enums.AnonymityMode;

public record CreateStoryRequest(
        String title,
        String body,
        String category,
        String region,
        String district,
        AnonymityMode anonymityMode,
        String displayName
) {
}
