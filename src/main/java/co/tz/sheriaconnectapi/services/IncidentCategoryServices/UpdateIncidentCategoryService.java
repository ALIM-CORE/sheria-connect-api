package co.tz.sheriaconnectapi.services.IncidentCategoryServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.InvalidIncidentCategoryException;
import co.tz.sheriaconnectapi.model.DTOs.IncidentCategoryResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpdateIncidentCategoryInput;
import co.tz.sheriaconnectapi.model.DTOs.UpdateIncidentCategoryRequest;
import co.tz.sheriaconnectapi.model.Entities.IncidentCategory;
import co.tz.sheriaconnectapi.repositories.IncidentCategoryRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class UpdateIncidentCategoryService
        implements Command<UpdateIncidentCategoryInput, IncidentCategoryResponse> {

    private final IncidentCategoryRepository repository;
    private final IncidentCategoryValidationService validationService;

    public UpdateIncidentCategoryService(
            IncidentCategoryRepository repository,
            IncidentCategoryValidationService validationService
    ) {
        this.repository = repository;
        this.validationService = validationService;
    }

    @Override
    public ResponseEntity<StandardResponse<IncidentCategoryResponse>> execute(
            UpdateIncidentCategoryInput input
    ) {
        if (input == null || input.request() == null) {
            throw new InvalidIncidentCategoryException("Incident category payload is required");
        }
        IncidentCategory category = validationService.requireExisting(input.code());
        UpdateIncidentCategoryRequest request = input.request();

        if (request.nameEn() != null) {
            if (request.nameEn().isBlank()) {
                throw new InvalidIncidentCategoryException("English category name cannot be blank");
            }
            category.setNameEn(request.nameEn().trim());
        }
        if (request.nameSw() != null) {
            category.setNameSw(trimToNull(request.nameSw()));
        }
        if (request.bodyEn() != null) {
            category.setBodyEn(trimToNull(request.bodyEn()));
        }
        if (request.bodySw() != null) {
            category.setBodySw(trimToNull(request.bodySw()));
        }
        if (request.selectable() != null) {
            category.setSelectable(request.selectable());
        }
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }

        IncidentCategory saved = repository.save(category);
        return ResponseUtil.success(
                new IncidentCategoryResponse(saved),
                "Incident category updated",
                HttpStatus.OK
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
