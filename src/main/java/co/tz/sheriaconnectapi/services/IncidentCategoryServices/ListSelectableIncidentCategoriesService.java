package co.tz.sheriaconnectapi.services.IncidentCategoryServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.IncidentCategoryResponse;
import co.tz.sheriaconnectapi.repositories.IncidentCategoryRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListSelectableIncidentCategoriesService
        implements Query<Void, List<IncidentCategoryResponse>> {

    private final IncidentCategoryRepository repository;

    public ListSelectableIncidentCategoriesService(IncidentCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public ResponseEntity<StandardResponse<List<IncidentCategoryResponse>>> execute(Void input) {
        List<IncidentCategoryResponse> categories = repository
                .findBySelectableTrueOrderBySortOrderAscCodeAsc()
                .stream()
                .map(IncidentCategoryResponse::new)
                .toList();
        return ResponseUtil.success(categories, "Incident categories retrieved", HttpStatus.OK);
    }
}
