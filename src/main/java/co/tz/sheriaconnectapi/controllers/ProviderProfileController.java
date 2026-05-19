package co.tz.sheriaconnectapi.controllers;

import co.tz.sheriaconnectapi.model.DTOs.CreateProviderProfileRequest;
import co.tz.sheriaconnectapi.model.DTOs.ProviderProfileResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpsertMyProviderProfileInput;
import co.tz.sheriaconnectapi.services.MatchingServices.GetMyProviderProfileService;
import co.tz.sheriaconnectapi.services.MatchingServices.UpsertMyProviderProfileService;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/provider-profile")
public class ProviderProfileController {

    private final GetMyProviderProfileService getMyProviderProfileService;
    private final UpsertMyProviderProfileService upsertMyProviderProfileService;

    public ProviderProfileController(
            GetMyProviderProfileService getMyProviderProfileService,
            UpsertMyProviderProfileService upsertMyProviderProfileService
    ) {
        this.getMyProviderProfileService = getMyProviderProfileService;
        this.upsertMyProviderProfileService = upsertMyProviderProfileService;
    }

    @GetMapping("/me")
    public ResponseEntity<StandardResponse<ProviderProfileResponse>> me(
            Authentication authentication
    ) {
        return getMyProviderProfileService.execute(authentication);
    }

    @PutMapping("/me")
    public ResponseEntity<StandardResponse<ProviderProfileResponse>> upsert(
            @RequestBody CreateProviderProfileRequest request,
            Authentication authentication
    ) {
        return upsertMyProviderProfileService.execute(
                new UpsertMyProviderProfileInput(request, authentication)
        );
    }
}
