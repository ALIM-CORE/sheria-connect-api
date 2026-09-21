package co.tz.sheriaconnectapi.services.IncidentCategoryServices;

import co.tz.sheriaconnectapi.exceptions.IncidentCategoryNotFoundException;
import co.tz.sheriaconnectapi.exceptions.InvalidIncidentCategoryException;
import co.tz.sheriaconnectapi.model.Entities.IncidentCategory;
import co.tz.sheriaconnectapi.repositories.IncidentCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentCategoryValidationServiceTest {

    @Mock
    private IncidentCategoryRepository repository;

    @Test
    void acceptsAndNormalizesASelectableCategory() {
        IncidentCategory category = category("DIGITAL_SAFETY", true);
        when(repository.findById("DIGITAL_SAFETY")).thenReturn(Optional.of(category));

        String result = service().requireSelectable(" digital_safety ");

        assertEquals("DIGITAL_SAFETY", result);
    }

    @Test
    void acceptsAllThreeLaunchCategories() {
        for (String code : Set.of(
                "GENDER_BASED_VIOLENCE",
                "CHILD_PROTECTION",
                "DIGITAL_SAFETY"
        )) {
            when(repository.findById(code)).thenReturn(Optional.of(category(code, true)));
            assertEquals(code, service().requireSelectable(code));
        }
    }

    @Test
    void rejectsARetiredCategoryForANewSelection() {
        when(repository.findById("TORTURE"))
                .thenReturn(Optional.of(category("TORTURE", false)));

        assertThrows(
                InvalidIncidentCategoryException.class,
                () -> service().requireSelectable("TORTURE")
        );
    }

    @Test
    void preservesAnExistingRetiredProviderSpecialty() {
        when(repository.findById("TORTURE"))
                .thenReturn(Optional.of(category("TORTURE", false)));
        when(repository.findById("CHILD_PROTECTION"))
                .thenReturn(Optional.of(category("CHILD_PROTECTION", true)));

        Set<String> result = service().validateSpecialties(
                Set.of("torture", "CHILD_PROTECTION"),
                Set.of("TORTURE")
        );

        assertEquals(Set.of("TORTURE", "CHILD_PROTECTION"), result);
    }

    @Test
    void rejectsAnUnknownCategory() {
        when(repository.findById("UNKNOWN_CATEGORY")).thenReturn(Optional.empty());

        assertThrows(
                IncidentCategoryNotFoundException.class,
                () -> service().requireSelectable("UNKNOWN_CATEGORY")
        );
    }

    private IncidentCategoryValidationService service() {
        return new IncidentCategoryValidationService(repository);
    }

    private IncidentCategory category(String code, boolean selectable) {
        IncidentCategory category = new IncidentCategory();
        category.setCode(code);
        category.setNameEn(code);
        category.setSelectable(selectable);
        return category;
    }
}
