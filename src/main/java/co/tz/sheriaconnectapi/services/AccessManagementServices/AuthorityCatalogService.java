package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.AuthorityCatalogItemResponse;
import co.tz.sheriaconnectapi.model.Entities.Authority;
import co.tz.sheriaconnectapi.repositories.AuthorityRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuthorityCatalogService implements Query<Void, List<AuthorityCatalogItemResponse>> {
    private static final Set<String> VISIBLE_RESOURCES = Set.of(
            "USER",
            "ROLE",
            "AUTHORITY",
            "INCIDENTREPORT",
            "EVIDENCEFILE",
            "CASESTATUSHISTORY",
            "ADMINCASENOTE",
            "PROVIDERPROFILE",
            "CASEMATCHREQUEST",
            "PUBLICSTORY",
            "STORYCONTENTREPORT",
            "STORYMODERATIONNOTE"
    );

    private static final Map<String, String> RESOURCE_LABELS = Map.ofEntries(
            Map.entry("USER", "User Accounts"),
            Map.entry("ROLE", "Staff Roles"),
            Map.entry("AUTHORITY", "Authority Catalogue"),
            Map.entry("INCIDENTREPORT", "Incident Reports"),
            Map.entry("EVIDENCEFILE", "Evidence Metadata"),
            Map.entry("CASESTATUSHISTORY", "Case Status History"),
            Map.entry("ADMINCASENOTE", "Administrative Case Notes"),
            Map.entry("PROVIDERPROFILE", "Provider Profiles"),
            Map.entry("CASEMATCHREQUEST", "Provider Matching"),
            Map.entry("PUBLICSTORY", "Public Stories"),
            Map.entry("STORYCONTENTREPORT", "Reported Story Content"),
            Map.entry("STORYMODERATIONNOTE", "Story Moderation Notes")
    );

    private static final Map<String, String> ACTION_LABELS = Map.of(
            "CREATE", "Create",
            "READ", "View",
            "UPDATE", "Update",
            "DELETE", "Delete"
    );

    private final AuthorityRepository authorityRepository;

    public AuthorityCatalogService(AuthorityRepository authorityRepository) {
        this.authorityRepository = authorityRepository;
    }

    @Override
    public ResponseEntity<StandardResponse<List<AuthorityCatalogItemResponse>>> execute(Void input) {
        List<AuthorityCatalogItemResponse> items = authorityRepository.findAll().stream()
                .map(this::map)
                .filter(java.util.Objects::nonNull)
                .sorted(java.util.Comparator
                        .comparing(AuthorityCatalogItemResponse::category)
                        .thenComparing(AuthorityCatalogItemResponse::resourceLabel)
                        .thenComparing(AuthorityCatalogItemResponse::actionLabel))
                .toList();
        return ResponseUtil.success(items, "Authority catalogue retrieved", HttpStatus.OK);
    }

    private AuthorityCatalogItemResponse map(Authority authority) {
        String name = authority.getName();
        int separator = name.lastIndexOf('_');
        if (separator < 1) {
            return null;
        }
        String resource = name.substring(0, separator);
        String action = name.substring(separator + 1);
        if (!VISIBLE_RESOURCES.contains(resource) || !ACTION_LABELS.containsKey(action)) {
            return null;
        }

        String resourceLabel = RESOURCE_LABELS.get(resource);
        String actionLabel = ACTION_LABELS.get(action);
        return new AuthorityCatalogItemResponse(
                authority.getId(),
                name,
                category(resource),
                resourceLabel,
                actionLabel,
                actionLabel + " " + resourceLabel,
                description(action, resourceLabel)
        );
    }

    private String category(String resource) {
        return switch (resource) {
            case "USER", "ROLE", "AUTHORITY" -> "Access Management";
            case "INCIDENTREPORT", "EVIDENCEFILE", "CASESTATUSHISTORY", "ADMINCASENOTE",
                 "CASEMATCHREQUEST" -> "Case Operations";
            case "PROVIDERPROFILE" -> "Provider Operations";
            default -> "Story Moderation";
        };
    }

    private String description(String action, String resourceLabel) {
        return switch (action) {
            case "CREATE" -> "Create new " + resourceLabel.toLowerCase() + ".";
            case "READ" -> "View " + resourceLabel.toLowerCase() + ".";
            case "UPDATE" -> "Change " + resourceLabel.toLowerCase() + ".";
            case "DELETE" -> "Remove " + resourceLabel.toLowerCase() + " where permitted.";
            default -> resourceLabel;
        };
    }
}
