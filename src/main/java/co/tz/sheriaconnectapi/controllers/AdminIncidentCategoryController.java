package co.tz.sheriaconnectapi.controllers;

import co.tz.sheriaconnectapi.model.DTOs.CreateIncidentCategoryRequest;
import co.tz.sheriaconnectapi.model.DTOs.IncidentCategoryResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpdateIncidentCategoryInput;
import co.tz.sheriaconnectapi.model.DTOs.UpdateIncidentCategoryRequest;
import co.tz.sheriaconnectapi.services.IncidentCategoryServices.CreateIncidentCategoryService;
import co.tz.sheriaconnectapi.services.IncidentCategoryServices.ListIncidentCategoriesService;
import co.tz.sheriaconnectapi.services.IncidentCategoryServices.UpdateIncidentCategoryService;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/incident-categories")
public class AdminIncidentCategoryController {

    private final ListIncidentCategoriesService listService;
    private final CreateIncidentCategoryService createService;
    private final UpdateIncidentCategoryService updateService;

    public AdminIncidentCategoryController(
            ListIncidentCategoriesService listService,
            CreateIncidentCategoryService createService,
            UpdateIncidentCategoryService updateService
    ) {
        this.listService = listService;
        this.createService = createService;
        this.updateService = updateService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INCIDENTCATEGORY_READ')")
    public ResponseEntity<StandardResponse<List<IncidentCategoryResponse>>> list() {
        return listService.execute(null);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INCIDENTCATEGORY_CREATE')")
    public ResponseEntity<StandardResponse<IncidentCategoryResponse>> create(
            @RequestBody CreateIncidentCategoryRequest request
    ) {
        return createService.execute(request);
    }

    @PatchMapping("/{code}")
    @PreAuthorize("hasAuthority('INCIDENTCATEGORY_UPDATE')")
    public ResponseEntity<StandardResponse<IncidentCategoryResponse>> update(
            @PathVariable String code,
            @RequestBody UpdateIncidentCategoryRequest request
    ) {
        return updateService.execute(new UpdateIncidentCategoryInput(code, request));
    }
}
