package co.tz.sheriaconnectapi.model.DTOs;

public record CreateIncidentCategoryRequest(
        String code,
        String nameEn,
        String nameSw,
        String bodyEn,
        String bodySw,
        Boolean selectable,
        Integer sortOrder
) {
}
