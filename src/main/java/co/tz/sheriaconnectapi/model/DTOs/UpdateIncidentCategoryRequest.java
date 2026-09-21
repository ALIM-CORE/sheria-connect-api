package co.tz.sheriaconnectapi.model.DTOs;

public record UpdateIncidentCategoryRequest(
        String nameEn,
        String nameSw,
        String bodyEn,
        String bodySw,
        Boolean selectable,
        Integer sortOrder
) {
}
