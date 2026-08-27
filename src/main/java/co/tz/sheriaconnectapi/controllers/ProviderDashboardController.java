package co.tz.sheriaconnectapi.controllers;

import co.tz.sheriaconnectapi.model.DTOs.ProviderTodayResponse;
import co.tz.sheriaconnectapi.services.MatchingServices.GetProviderTodayService;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/provider")
public class ProviderDashboardController {
    private final GetProviderTodayService getProviderTodayService;

    public ProviderDashboardController(GetProviderTodayService getProviderTodayService) {
        this.getProviderTodayService = getProviderTodayService;
    }

    @GetMapping("/today")
    public ResponseEntity<StandardResponse<ProviderTodayResponse>> today(
            Authentication authentication
    ) {
        return getProviderTodayService.execute(authentication);
    }
}
