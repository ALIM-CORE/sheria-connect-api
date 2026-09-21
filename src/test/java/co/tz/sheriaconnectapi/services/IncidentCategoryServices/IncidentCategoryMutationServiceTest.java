package co.tz.sheriaconnectapi.services.IncidentCategoryServices;

import co.tz.sheriaconnectapi.exceptions.DuplicateIncidentCategoryException;
import co.tz.sheriaconnectapi.model.DTOs.CreateIncidentCategoryRequest;
import co.tz.sheriaconnectapi.model.DTOs.UpdateIncidentCategoryInput;
import co.tz.sheriaconnectapi.model.DTOs.UpdateIncidentCategoryRequest;
import co.tz.sheriaconnectapi.model.Entities.IncidentCategory;
import co.tz.sheriaconnectapi.repositories.IncidentCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentCategoryMutationServiceTest {

    @Mock
    private IncidentCategoryRepository repository;

    @Test
    void createsANormalizedCategoryCode() {
        IncidentCategoryValidationService validation = validation();
        when(repository.existsById("WORKPLACE_ABUSE")).thenReturn(false);
        when(repository.save(any(IncidentCategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new CreateIncidentCategoryService(repository, validation).execute(
                new CreateIncidentCategoryRequest(
                        " workplace_abuse ",
                        "Workplace abuse",
                        "Ukatili kazini",
                        null,
                        null,
                        true,
                        40
                )
        );

        ArgumentCaptor<IncidentCategory> captor =
                ArgumentCaptor.forClass(IncidentCategory.class);
        verify(repository).save(captor.capture());
        assertEquals("WORKPLACE_ABUSE", captor.getValue().getCode());
        assertEquals(true, captor.getValue().isSelectable());
    }

    @Test
    void rejectsADuplicateCategoryCode() {
        when(repository.existsById("DIGITAL_SAFETY")).thenReturn(true);

        assertThrows(
                DuplicateIncidentCategoryException.class,
                () -> new CreateIncidentCategoryService(repository, validation()).execute(
                        new CreateIncidentCategoryRequest(
                                "DIGITAL_SAFETY",
                                "Digital safety",
                                null,
                                null,
                                null,
                                true,
                                10
                        )
                )
        );
    }

    @Test
    void updateCannotChangeTheImmutableCode() {
        IncidentCategory category = new IncidentCategory();
        category.setCode("CHILD_PROTECTION");
        category.setNameEn("Old name");
        when(repository.findById("CHILD_PROTECTION")).thenReturn(Optional.of(category));
        when(repository.save(any(IncidentCategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new UpdateIncidentCategoryService(repository, validation()).execute(
                new UpdateIncidentCategoryInput(
                        "CHILD_PROTECTION",
                        new UpdateIncidentCategoryRequest(
                                "Child protection",
                                null,
                                null,
                                null,
                                false,
                                25
                        )
                )
        );

        assertEquals("CHILD_PROTECTION", category.getCode());
        assertEquals("Child protection", category.getNameEn());
        assertEquals(false, category.isSelectable());
    }

    private IncidentCategoryValidationService validation() {
        return new IncidentCategoryValidationService(repository);
    }
}
