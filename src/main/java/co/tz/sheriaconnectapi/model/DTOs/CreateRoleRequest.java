package co.tz.sheriaconnectapi.model.DTOs;

import java.util.Set;

public record CreateRoleRequest(
        String name,
        String displayName,
        String description,
        Set<String> authorityNames
) {
}
