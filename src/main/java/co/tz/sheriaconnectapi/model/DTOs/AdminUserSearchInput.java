package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Enums.AccessContext;

public record AdminUserSearchInput(
        AccessContext capability,
        String search,
        Boolean active,
        Boolean locked,
        Long roleId,
        int page,
        int size,
        String sortBy,
        String sortDirection,
        boolean includeLinkedIdentity
) {
}
