package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.model.DTOs.CreateProviderProfileRequest;
import co.tz.sheriaconnectapi.model.DTOs.UpsertMyProviderProfileInput;
import co.tz.sheriaconnectapi.model.Entities.IncidentCategory;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.LegalServiceProviderType;
import co.tz.sheriaconnectapi.repositories.IncidentCategoryRepository;
import co.tz.sheriaconnectapi.repositories.ProviderProfileRepository;
import co.tz.sheriaconnectapi.services.IncidentCategoryServices.IncidentCategoryValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpsertMyProviderProfileServiceTest {

    @Mock
    private ProviderProfileRepository providerProfileRepository;
    @Mock
    private ProviderProfileAccessService providerProfileAccessService;
    @Mock
    private IncidentCategoryRepository incidentCategoryRepository;
    @Mock
    private Authentication authentication;

    @Test
    void preservesAnExistingRetiredSpecialtyDuringProfileUpdate() {
        User user = new User();
        user.setId(9L);

        ProviderProfile profile = new ProviderProfile();
        profile.setId(15L);
        profile.setUser(user);
        profile.setProviderType(LegalServiceProviderType.ADVOCATE);
        profile.setDisplayName("Amina Legal Aid");
        profile.setSpecialties(new java.util.HashSet<>(Set.of("TORTURE")));

        CreateProviderProfileRequest request = new CreateProviderProfileRequest();
        request.setProviderType(LegalServiceProviderType.ADVOCATE);
        request.setDisplayName("Amina Legal Aid");
        request.setSpecialties(Set.of("TORTURE", "CHILD_PROTECTION"));

        when(providerProfileAccessService.requireAuthenticatedUser(authentication))
                .thenReturn(user);
        when(providerProfileRepository.findFirstByUserOrderByCreatedAtDesc(user))
                .thenReturn(Optional.of(profile));
        when(incidentCategoryRepository.findById("TORTURE"))
                .thenReturn(Optional.of(category("TORTURE", false)));
        when(incidentCategoryRepository.findById("CHILD_PROTECTION"))
                .thenReturn(Optional.of(category("CHILD_PROTECTION", true)));
        when(providerProfileRepository.save(any(ProviderProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpsertMyProviderProfileService service = new UpsertMyProviderProfileService(
                providerProfileRepository,
                providerProfileAccessService,
                new IncidentCategoryValidationService(incidentCategoryRepository)
        );

        service.execute(new UpsertMyProviderProfileInput(request, authentication));

        assertEquals(Set.of("TORTURE", "CHILD_PROTECTION"), profile.getSpecialties());
    }

    private IncidentCategory category(String code, boolean selectable) {
        IncidentCategory category = new IncidentCategory();
        category.setCode(code);
        category.setNameEn(code);
        category.setSelectable(selectable);
        return category;
    }
}
