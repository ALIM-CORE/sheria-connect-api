package co.tz.sheriaconnectapi.controllers;

import co.tz.sheriaconnectapi.model.DTOs.AcceptStaffInvitationRequest;
import co.tz.sheriaconnectapi.model.DTOs.AcceptExistingStaffInvitationRequest;
import co.tz.sheriaconnectapi.model.Commands.LoginResponse;
import co.tz.sheriaconnectapi.model.DTOs.StaffInvitationValidationResponse;
import co.tz.sheriaconnectapi.model.DTOs.UserDTO;
import co.tz.sheriaconnectapi.services.AccessManagementServices.StaffInvitationService;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/staff-invitations")
public class StaffInvitationController {
    private final StaffInvitationService invitationService;

    public StaffInvitationController(StaffInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping("/validate")
    public ResponseEntity<StandardResponse<StaffInvitationValidationResponse>> validate(
            @RequestParam String token
    ) {
        return invitationService.validate(token);
    }

    @PostMapping("/accept")
    public ResponseEntity<StandardResponse<UserDTO>> accept(
            @RequestBody AcceptStaffInvitationRequest request
    ) {
        return invitationService.accept(request);
    }

    @PostMapping("/accept-existing")
    public ResponseEntity<StandardResponse<LoginResponse>> acceptExisting(
            @RequestBody AcceptExistingStaffInvitationRequest request
    ) {
        return invitationService.acceptExisting(request);
    }
}
