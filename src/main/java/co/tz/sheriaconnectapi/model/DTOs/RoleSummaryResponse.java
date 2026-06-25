package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.Role;
import co.tz.sheriaconnectapi.model.Enums.RoleAudience;

import java.util.List;

public record RoleSummaryResponse(
        Long id,
        String name,
        String displayName,
        String description,
        RoleAudience audience,
        boolean systemRole,
        boolean editable,
        boolean deletable,
        long userCount,
        List<String> authorityNames
) {
    public RoleSummaryResponse(Role role, long userCount) {
        this(
                role.getId(),
                role.getName(),
                role.getDisplayName(),
                role.getDescription(),
                role.getAudience(),
                role.isSystemRole(),
                role.isEditable(),
                role.isDeletable(),
                userCount,
                role.getAuthorities().stream()
                        .map(authority -> authority.getName())
                        .sorted()
                        .toList()
        );
    }
}
