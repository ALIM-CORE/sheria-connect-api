package co.tz.sheriaconnectapi.services.IncidentCategoryServices;

import co.tz.sheriaconnectapi.exceptions.IncidentCategoryNotFoundException;
import co.tz.sheriaconnectapi.exceptions.InvalidIncidentCategoryException;
import co.tz.sheriaconnectapi.model.Entities.IncidentCategory;
import co.tz.sheriaconnectapi.repositories.IncidentCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class IncidentCategoryValidationService {
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,63}$");

    private final IncidentCategoryRepository repository;

    public IncidentCategoryValidationService(IncidentCategoryRepository repository) {
        this.repository = repository;
    }

    public String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidIncidentCategoryException("Incident category is required");
        }
        String code = value.trim().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new InvalidIncidentCategoryException(
                    "Incident category code must use uppercase letters, numbers, and underscores"
            );
        }
        return code;
    }

    public IncidentCategory requireExisting(String value) {
        return repository.findById(normalizeCode(value))
                .orElseThrow(IncidentCategoryNotFoundException::new);
    }

    public String requireSelectable(String value) {
        IncidentCategory category = requireExisting(value);
        if (!category.isSelectable()) {
            throw new InvalidIncidentCategoryException(
                    "This incident category is not available for new selections"
            );
        }
        return category.getCode();
    }

    public Set<String> validateSpecialties(
            Set<String> requested,
            Set<String> existingSpecialties
    ) {
        if (requested == null || requested.isEmpty()) {
            return Set.of();
        }

        Set<String> existing = new LinkedHashSet<>();
        if (existingSpecialties != null) {
            existingSpecialties.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(this::normalizeCode)
                    .forEach(existing::add);
        }

        Set<String> validated = new LinkedHashSet<>();
        for (String value : requested) {
            IncidentCategory category = requireExisting(value);
            if (!category.isSelectable() && !existing.contains(category.getCode())) {
                throw new InvalidIncidentCategoryException(
                        "Retired incident categories cannot be added as new specialties"
                );
            }
            validated.add(category.getCode());
        }
        return validated;
    }
}
