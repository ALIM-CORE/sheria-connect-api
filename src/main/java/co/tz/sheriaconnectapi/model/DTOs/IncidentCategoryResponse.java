package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.IncidentCategory;

import java.time.Instant;

public record IncidentCategoryResponse(
        String code,
        String nameEn,
        String nameSw,
        String bodyEn,
        String bodySw,
        boolean selectable,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public IncidentCategoryResponse(IncidentCategory category) {
        this(
                category.getCode(),
                category.getNameEn(),
                category.getNameSw(),
                category.getBodyEn(),
                category.getBodySw(),
                category.isSelectable(),
                category.getSortOrder(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
