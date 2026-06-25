package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.exceptions.AccessManagementException;
import co.tz.sheriaconnectapi.model.DTOs.CreateRoleRequest;
import co.tz.sheriaconnectapi.model.DTOs.RoleSummaryResponse;
import co.tz.sheriaconnectapi.model.Entities.Authority;
import co.tz.sheriaconnectapi.model.Entities.Role;
import co.tz.sheriaconnectapi.model.Enums.RoleAudience;
import co.tz.sheriaconnectapi.repositories.AuthorityRepository;
import co.tz.sheriaconnectapi.repositories.RoleRepository;
import co.tz.sheriaconnectapi.repositories.UserRoleAssignmentRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class RoleMutationService {
    private final RoleRepository roleRepository;
    private final AuthorityRepository authorityRepository;
    private final UserRoleAssignmentRepository assignmentRepository;
    private final AccessManagementPolicy policy;
    private final AccessAuditService auditService;

    public RoleMutationService(
            RoleRepository roleRepository,
            AuthorityRepository authorityRepository,
            UserRoleAssignmentRepository assignmentRepository,
            AccessManagementPolicy policy,
            AccessAuditService auditService
    ) {
        this.roleRepository = roleRepository;
        this.authorityRepository = authorityRepository;
        this.assignmentRepository = assignmentRepository;
        this.policy = policy;
        this.auditService = auditService;
    }

    @Transactional
    public ResponseEntity<StandardResponse<RoleSummaryResponse>> create(
            CreateRoleRequest request,
            Authentication authentication
    ) {
        var actor = policy.requireActor(authentication);
        String name = normalizeName(request == null ? null : request.name());
        String displayName = normalizeDisplayName(request == null ? null : request.displayName());
        if (name == null || displayName == null) {
            throw badRequest("Role name and display name are required");
        }
        if (roleRepository.findByName(name).isPresent()) {
            throw new AccessManagementException("A role with this name already exists", HttpStatus.CONFLICT);
        }

        Role role = new Role();
        role.setName(name);
        role.setDisplayName(displayName);
        role.setDescription(trimToNull(request.description()));
        role.setAudience(RoleAudience.PLATFORM_STAFF);
        role.setSystemRole(false);
        role.setEditable(true);
        role.setDeletable(true);
        role.setAuthorities(resolveAuthorities(request.authorityNames()));
        Role saved = roleRepository.save(role);
        auditService.log(actor, null, "ROLE_CREATED", "ROLE", saved.getId(), saved.getName());
        return ResponseUtil.success(
                new RoleSummaryResponse(saved, 0),
                "Role created",
                HttpStatus.CREATED
        );
    }

    @Transactional
    public ResponseEntity<StandardResponse<RoleSummaryResponse>> update(
            Long roleId,
            CreateRoleRequest request,
            Authentication authentication
    ) {
        var actor = policy.requireActor(authentication);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AccessManagementException("Role not found", HttpStatus.NOT_FOUND));
        if (!role.isEditable() || role.getAudience() != RoleAudience.PLATFORM_STAFF) {
            throw new AccessManagementException("This protected role cannot be edited", HttpStatus.FORBIDDEN);
        }

        String name = normalizeName(request == null ? null : request.name());
        String displayName = normalizeDisplayName(request == null ? null : request.displayName());
        if (displayName == null) {
            throw badRequest("Display name is required");
        }
        if (!role.isSystemRole() && name != null && !name.equals(role.getName())) {
            roleRepository.findByName(name)
                    .filter(existing -> !existing.getId().equals(roleId))
                    .ifPresent(existing -> {
                        throw new AccessManagementException(
                                "A role with this name already exists",
                                HttpStatus.CONFLICT
                        );
                    });
            role.setName(name);
        }
        role.setDisplayName(displayName);
        role.setDescription(trimToNull(request.description()));
        role.setAuthorities(resolveAuthorities(request.authorityNames()));
        Role saved = roleRepository.save(role);
        auditService.log(actor, null, "ROLE_UPDATED", "ROLE", saved.getId(), saved.getName());
        return ResponseUtil.success(
                new RoleSummaryResponse(
                        saved,
                        assignmentRepository.countActiveUsersByRole(saved.getId(), Instant.now())
                ),
                "Role updated",
                HttpStatus.OK
        );
    }

    @Transactional
    public ResponseEntity<StandardResponse<Void>> delete(
            Long roleId,
            Authentication authentication
    ) {
        var actor = policy.requireActor(authentication);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AccessManagementException("Role not found", HttpStatus.NOT_FOUND));
        if (role.isSystemRole() || !role.isDeletable()) {
            throw new AccessManagementException("Protected roles cannot be deleted", HttpStatus.FORBIDDEN);
        }
        if (assignmentRepository.existsByRole_Id(roleId)) {
            throw badRequest("This role has assignment history and cannot be deleted");
        }
        roleRepository.delete(role);
        auditService.log(actor, null, "ROLE_DELETED", "ROLE", roleId, role.getName());
        return ResponseUtil.success(null, "Role deleted", HttpStatus.OK);
    }

    private Set<Authority> resolveAuthorities(Set<String> names) {
        Set<Authority> authorities = new HashSet<>();
        if (names == null) {
            return authorities;
        }
        for (String value : names) {
            String name = value == null ? null : value.trim().toUpperCase(Locale.ROOT);
            if (name == null || name.isBlank()) {
                throw badRequest("Authority names must not be blank");
            }
            Authority authority = authorityRepository.findByName(name)
                    .orElseThrow(() -> badRequest("Authority not found: " + name));
            authorities.add(authority);
        }
        return authorities;
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", "_").toUpperCase(Locale.ROOT);
    }

    private String normalizeDisplayName(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private AccessManagementException badRequest(String message) {
        return new AccessManagementException(message, HttpStatus.BAD_REQUEST);
    }
}
