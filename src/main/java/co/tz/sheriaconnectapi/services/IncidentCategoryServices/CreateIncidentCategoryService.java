package co.tz.sheriaconnectapi.services.IncidentCategoryServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.DuplicateIncidentCategoryException;
import co.tz.sheriaconnectapi.exceptions.InvalidIncidentCategoryException;
import co.tz.sheriaconnectapi.model.DTOs.CreateIncidentCategoryRequest;
import co.tz.sheriaconnectapi.model.DTOs.IncidentCategoryResponse;
import co.tz.sheriaconnectapi.model.Entities.IncidentCategory;
import co.tz.sheriaconnectapi.repositories.IncidentCategoryRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CreateIncidentCategoryService
        implements Command<CreateIncidentCategoryRequest, IncidentCategoryResponse> {

    private final IncidentCategoryRepository repository;
    private final IncidentCategoryValidationService validationService;

    public CreateIncidentCategoryService(
            IncidentCategoryRepository repository,
            IncidentCategoryValidationService validationService
    ) {
        this.repository = repository;
        this.validationService = validationService;
    }

    @Override
    public ResponseEntity<StandardResponse<IncidentCategoryResponse>> execute(
            CreateIncidentCategoryRequest request
    ) {
        if (request == null) {
            throw new InvalidIncidentCategoryException("Incident category payload is required");
        }
        String code = validationService.normalizeCode(request.code());
        if (repository.existsById(code)) {
            throw new DuplicateIncidentCategoryException();
        }

        IncidentCategory category = new IncidentCategory();
        category.setCode(code);
        category.setNameEn(requiredName(request.nameEn()));
        category.setNameSw(trimToNull(request.nameSw()));
        category.setBodyEn(trimToNull(request.bodyEn()));
        category.setBodySw(trimToNull(request.bodySw()));
        category.setSelectable(Boolean.TRUE.equals(request.selectable()));
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());

        IncidentCategory saved = repository.save(category);
        return ResponseUtil.success(
                new IncidentCategoryResponse(saved),
                "Incident category created",
                HttpStatus.CREATED
        );
    }

    private String requiredName(String value) {
        String name = trimToNull(value);
        if (name == null) {
            throw new InvalidIncidentCategoryException("English category name is required");
        }
        return name;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
