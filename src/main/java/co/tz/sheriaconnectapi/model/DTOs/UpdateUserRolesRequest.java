package co.tz.sheriaconnectapi.model.DTOs;

import java.util.Set;

public record UpdateUserRolesRequest(Set<Long> roleIds, String reason) {
}
