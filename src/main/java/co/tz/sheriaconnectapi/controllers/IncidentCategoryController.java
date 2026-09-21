package co.tz.sheriaconnectapi.controllers;

import co.tz.sheriaconnectapi.model.DTOs.IncidentCategoryResponse;
import co.tz.sheriaconnectapi.services.IncidentCategoryServices.ListSelectableIncidentCategoriesService;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/incident-categories")
public class IncidentCategoryController {

    private final ListSelectableIncidentCategoriesService listService;

    public IncidentCategoryController(ListSelectableIncidentCategoriesService listService) {
        this.listService = listService;
    }

    @GetMapping
    public ResponseEntity<StandardResponse<List<IncidentCategoryResponse>>> list() {
        return listService.execute(null);
    }
}
