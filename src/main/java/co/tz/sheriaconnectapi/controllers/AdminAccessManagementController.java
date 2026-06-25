package co.tz.sheriaconnectapi.controllers;

import co.tz.sheriaconnectapi.model.DTOs.*;
import co.tz.sheriaconnectapi.model.Enums.RoleAudience;
import co.tz.sheriaconnectapi.model.Enums.UserAccountType;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.services.AccessManagementServices.*;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminAccessManagementController {
    private final ListAdminUsersService listUsersService;
    private final GetAdminUserService getUserService;
    private final GetAdminUserSummaryService getUserSummaryService;
    private final UpdateAdminUserStatusService updateUserStatusService;
    private final UpdateAdminUserRolesService updateUserRolesService;
    private final ListRolesService listRolesService;
    private final RoleMutationService roleMutationService;
    private final AuthorityCatalogService authorityCatalogService;
    private final StaffInvitationService invitationService;
    private final AccessManagementPolicy accessManagementPolicy;
    private final UpdateStaffAccessService updateStaffAccessService;
    private final StaffAssignmentQueryService staffAssignmentQueryService;
    private final ResetStaffMfaService resetStaffMfaService;

    public AdminAccessManagementController(
            ListAdminUsersService listUsersService,
            GetAdminUserService getUserService,
            GetAdminUserSummaryService getUserSummaryService,
            UpdateAdminUserStatusService updateUserStatusService,
            UpdateAdminUserRolesService updateUserRolesService,
            ListRolesService listRolesService,
            RoleMutationService roleMutationService,
            AuthorityCatalogService authorityCatalogService,
            StaffInvitationService invitationService,
            AccessManagementPolicy accessManagementPolicy,
            UpdateStaffAccessService updateStaffAccessService,
            StaffAssignmentQueryService staffAssignmentQueryService,
            ResetStaffMfaService resetStaffMfaService
    ) {
        this.listUsersService = listUsersService;
        this.getUserService = getUserService;
        this.getUserSummaryService = getUserSummaryService;
        this.updateUserStatusService = updateUserStatusService;
        this.updateUserRolesService = updateUserRolesService;
        this.listRolesService = listRolesService;
        this.roleMutationService = roleMutationService;
        this.authorityCatalogService = authorityCatalogService;
        this.invitationService = invitationService;
        this.accessManagementPolicy = accessManagementPolicy;
        this.updateStaffAccessService = updateStaffAccessService;
        this.staffAssignmentQueryService = staffAssignmentQueryService;
        this.resetStaffMfaService = resetStaffMfaService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<StandardResponse<PageResponse<AdminUserListItemResponse>>> listUsers(
            @RequestParam(required = false) AccessContext capability,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean locked,
            @RequestParam(required = false) Long roleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            Authentication authentication
    ) {
        return listUsersService.execute(new AdminUserSearchInput(
                capability,
                search,
                active,
                locked,
                roleId,
                page,
                size,
                sortBy,
                sortDirection,
                accessManagementPolicy.canViewLinkedIdentity(authentication)
        ));
    }

    @GetMapping("/users/summary")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<StandardResponse<AdminUserSummaryResponse>> userSummary() {
        return getUserSummaryService.execute(null);
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<StandardResponse<AdminUserListItemResponse>> getUser(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        return getUserService.execute(userId, authentication);
    }

    @PatchMapping("/users/{userId}/status")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<StandardResponse<AdminUserListItemResponse>> updateStatus(
            @PathVariable Long userId,
            @RequestBody UpdateUserStatusRequest request,
            Authentication authentication
    ) {
        return updateUserStatusService.execute(userId, request, authentication);
    }

    @PatchMapping("/users/{userId}/roles")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<StandardResponse<AdminUserListItemResponse>> updateRoles(
            @PathVariable Long userId,
            @RequestBody UpdateUserRolesRequest request,
            Authentication authentication
    ) {
        return updateUserRolesService.execute(userId, request, authentication);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('USER_CREATE', 'ROLE_READ')")
    public ResponseEntity<StandardResponse<List<RoleSummaryResponse>>> listRoles(
            @RequestParam(required = false) RoleAudience audience
    ) {
        return listRolesService.execute(audience);
    }

    @PostMapping("/roles")
    @PreAuthorize("@accessManagementPolicy.isSuperAdmin(authentication)")
    public ResponseEntity<StandardResponse<RoleSummaryResponse>> createRole(
            @RequestBody CreateRoleRequest request,
            Authentication authentication
    ) {
        return roleMutationService.create(request, authentication);
    }

    @PutMapping("/roles/{roleId}")
    @PreAuthorize("@accessManagementPolicy.isSuperAdmin(authentication)")
    public ResponseEntity<StandardResponse<RoleSummaryResponse>> updateRole(
            @PathVariable Long roleId,
            @RequestBody CreateRoleRequest request,
            Authentication authentication
    ) {
        return roleMutationService.update(roleId, request, authentication);
    }

    @DeleteMapping("/roles/{roleId}")
    @PreAuthorize("@accessManagementPolicy.isSuperAdmin(authentication)")
    public ResponseEntity<StandardResponse<Void>> deleteRole(
            @PathVariable Long roleId,
            Authentication authentication
    ) {
        return roleMutationService.delete(roleId, authentication);
    }

    @GetMapping("/authorities/catalog")
    @PreAuthorize("@accessManagementPolicy.isSuperAdmin(authentication)")
    public ResponseEntity<StandardResponse<List<AuthorityCatalogItemResponse>>> authorityCatalog() {
        return authorityCatalogService.execute(null);
    }

    @GetMapping("/staff-invitations")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<StandardResponse<List<StaffInvitationResponse>>> listInvitations() {
        return invitationService.list();
    }

    @PostMapping("/staff-invitations")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<StandardResponse<StaffInvitationResponse>> createInvitation(
            @RequestBody CreateStaffInvitationRequest request,
            Authentication authentication
    ) {
        return invitationService.create(request, authentication);
    }

    @PostMapping("/users/{productUserId}/staff-invitations")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<StandardResponse<StaffInvitationResponse>> createLinkedInvitation(
            @PathVariable Long productUserId,
            @RequestBody CreateLinkedStaffInvitationRequest request,
            Authentication authentication
    ) {
        return invitationService.createLinked(productUserId, request, authentication);
    }

    @PostMapping("/staff-invitations/{invitationId}/resend")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<StandardResponse<StaffInvitationResponse>> resendInvitation(
            @PathVariable Long invitationId,
            Authentication authentication
    ) {
        return invitationService.resend(invitationId, authentication);
    }

    @PostMapping("/staff-invitations/{invitationId}/revoke")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<StandardResponse<StaffInvitationResponse>> revokeInvitation(
            @PathVariable Long invitationId,
            Authentication authentication
    ) {
        return invitationService.revoke(invitationId, authentication);
    }

    @GetMapping("/users/{userId}/staff-assignments")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<StandardResponse<List<RoleAssignmentResponse>>> staffAssignments(
            @PathVariable Long userId
    ) {
        return staffAssignmentQueryService.list(userId);
    }

    @PatchMapping("/users/{userId}/staff-access")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<StandardResponse<AdminUserListItemResponse>> updateStaffAccess(
            @PathVariable Long userId,
            @RequestBody UpdateStaffAccessRequest request,
            Authentication authentication
    ) {
        return updateStaffAccessService.execute(userId, request, authentication);
    }

    @PostMapping("/users/{userId}/staff-mfa/reset")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<StandardResponse<Void>> resetStaffMfa(
            @PathVariable Long userId,
            @RequestBody java.util.Map<String, String> request,
            Authentication authentication
    ) {
        return resetStaffMfaService.execute(
                userId,
                request == null ? null : request.get("reason"),
                authentication
        );
    }
}
