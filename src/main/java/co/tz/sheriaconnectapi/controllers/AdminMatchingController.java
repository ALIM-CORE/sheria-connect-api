package co.tz.sheriaconnectapi.controllers;

import co.tz.sheriaconnectapi.exceptions.InvalidCaseNumberException;
import co.tz.sheriaconnectapi.model.DTOs.CreateMatchingRequestInput;
import co.tz.sheriaconnectapi.model.DTOs.CreateMatchingRequestRequest;
import co.tz.sheriaconnectapi.model.DTOs.CreateProviderProfileRequest;
import co.tz.sheriaconnectapi.model.DTOs.MatchingRecommendationResponse;
import co.tz.sheriaconnectapi.model.DTOs.MatchingRequestResponse;
import co.tz.sheriaconnectapi.model.DTOs.ProviderProfileResponse;
import co.tz.sheriaconnectapi.model.DTOs.ProviderProfileSearchInput;
import co.tz.sheriaconnectapi.model.DTOs.UpdateMatchingRequestStatusInput;
import co.tz.sheriaconnectapi.model.DTOs.UpdateMatchingRequestStatusRequest;
import co.tz.sheriaconnectapi.model.DTOs.UpdateProviderVerificationInput;
import co.tz.sheriaconnectapi.model.DTOs.UpdateProviderVerificationRequest;
import co.tz.sheriaconnectapi.model.Enums.LegalServiceProviderType;
import co.tz.sheriaconnectapi.model.Enums.ProviderAvailabilityStatus;
import co.tz.sheriaconnectapi.model.Enums.ProviderVerificationStatus;
import co.tz.sheriaconnectapi.services.MatchingServices.CreateMatchingRequestService;
import co.tz.sheriaconnectapi.services.MatchingServices.CreateProviderProfileService;
import co.tz.sheriaconnectapi.services.MatchingServices.ListCaseMatchingRequestsService;
import co.tz.sheriaconnectapi.services.MatchingServices.ListProviderProfilesService;
import co.tz.sheriaconnectapi.services.MatchingServices.RecommendProvidersForIncidentService;
import co.tz.sheriaconnectapi.services.MatchingServices.UpdateMatchingRequestStatusService;
import co.tz.sheriaconnectapi.services.MatchingServices.UpdateProviderVerificationService;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/admin/matching")
public class AdminMatchingController {

    private static final Pattern CASE_NUMBER_PATTERN =
            Pattern.compile("^SC-\\d{4}-[A-Z2-9]{6}$");

    private final CreateProviderProfileService createProviderProfileService;
    private final ListProviderProfilesService listProviderProfilesService;
    private final UpdateProviderVerificationService updateProviderVerificationService;
    private final RecommendProvidersForIncidentService recommendProvidersForIncidentService;
    private final CreateMatchingRequestService createMatchingRequestService;
    private final ListCaseMatchingRequestsService listCaseMatchingRequestsService;
    private final UpdateMatchingRequestStatusService updateMatchingRequestStatusService;

    public AdminMatchingController(
            CreateProviderProfileService createProviderProfileService,
            ListProviderProfilesService listProviderProfilesService,
            UpdateProviderVerificationService updateProviderVerificationService,
            RecommendProvidersForIncidentService recommendProvidersForIncidentService,
            CreateMatchingRequestService createMatchingRequestService,
            ListCaseMatchingRequestsService listCaseMatchingRequestsService,
            UpdateMatchingRequestStatusService updateMatchingRequestStatusService
    ) {
        this.createProviderProfileService = createProviderProfileService;
        this.listProviderProfilesService = listProviderProfilesService;
        this.updateProviderVerificationService = updateProviderVerificationService;
        this.recommendProvidersForIncidentService = recommendProvidersForIncidentService;
        this.createMatchingRequestService = createMatchingRequestService;
        this.listCaseMatchingRequestsService = listCaseMatchingRequestsService;
        this.updateMatchingRequestStatusService = updateMatchingRequestStatusService;
    }

    @PostMapping("/provider-profiles")
    @PreAuthorize("hasAuthority('PROVIDERPROFILE_CREATE')")
    public ResponseEntity<StandardResponse<ProviderProfileResponse>> createProviderProfile(
            @RequestBody CreateProviderProfileRequest request
    ) {
        return createProviderProfileService.execute(request);
    }

    @GetMapping("/provider-profiles")
    @PreAuthorize("hasAuthority('PROVIDERPROFILE_READ')")
    public ResponseEntity<StandardResponse<List<ProviderProfileResponse>>> listProviderProfiles(
            @RequestParam(required = false) LegalServiceProviderType providerType,
            @RequestParam(required = false) ProviderVerificationStatus verificationStatus,
            @RequestParam(required = false) ProviderAvailabilityStatus availabilityStatus,
            @RequestParam(required = false) Boolean active
    ) {
        return listProviderProfilesService.execute(
                new ProviderProfileSearchInput(
                        providerType,
                        verificationStatus,
                        availabilityStatus,
                        active
                )
        );
    }

    @PatchMapping("/provider-profiles/{providerProfileId}/verification")
    @PreAuthorize("hasAuthority('PROVIDERPROFILE_UPDATE')")
    public ResponseEntity<StandardResponse<ProviderProfileResponse>> updateProviderVerification(
            @PathVariable Long providerProfileId,
            @RequestBody UpdateProviderVerificationRequest request
    ) {
        return updateProviderVerificationService.execute(
                new UpdateProviderVerificationInput(providerProfileId, request)
        );
    }

    @GetMapping("/incident-reports/{caseNumber}/recommendations")
    @PreAuthorize("hasAuthority('CASEMATCHREQUEST_READ')")
    public ResponseEntity<StandardResponse<List<MatchingRecommendationResponse>>> recommendations(
            @PathVariable String caseNumber
    ) {
        validateCaseNumber(caseNumber);
        return recommendProvidersForIncidentService.execute(caseNumber);
    }

    @GetMapping("/incident-reports/{caseNumber}/requests")
    @PreAuthorize("hasAuthority('CASEMATCHREQUEST_READ')")
    public ResponseEntity<StandardResponse<List<MatchingRequestResponse>>> listRequests(
            @PathVariable String caseNumber
    ) {
        validateCaseNumber(caseNumber);
        return listCaseMatchingRequestsService.execute(caseNumber);
    }

    @PostMapping("/incident-reports/{caseNumber}/requests")
    @PreAuthorize("hasAuthority('CASEMATCHREQUEST_CREATE')")
    public ResponseEntity<StandardResponse<MatchingRequestResponse>> createRequest(
            @PathVariable String caseNumber,
            @RequestBody CreateMatchingRequestRequest request,
            Authentication authentication
    ) {
        validateCaseNumber(caseNumber);
        return createMatchingRequestService.execute(
                new CreateMatchingRequestInput(caseNumber, request, authentication)
        );
    }

    @PatchMapping("/requests/{matchingRequestId}/status")
    @PreAuthorize("hasAuthority('CASEMATCHREQUEST_UPDATE')")
    public ResponseEntity<StandardResponse<MatchingRequestResponse>> updateRequestStatus(
            @PathVariable Long matchingRequestId,
            @RequestBody UpdateMatchingRequestStatusRequest request,
            Authentication authentication
    ) {
        return updateMatchingRequestStatusService.execute(
                new UpdateMatchingRequestStatusInput(
                        matchingRequestId,
                        request,
                        authentication
                )
        );
    }

    private void validateCaseNumber(String caseNumber) {
        if (caseNumber == null || !CASE_NUMBER_PATTERN.matcher(caseNumber).matches()) {
            throw new InvalidCaseNumberException();
        }
    }
}
