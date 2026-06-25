package co.tz.sheriaconnectapi.model.DTOs;

public record AuthorityCatalogItemResponse(
        int id,
        String name,
        String category,
        String resourceLabel,
        String actionLabel,
        String displayName,
        String description
) {
}
