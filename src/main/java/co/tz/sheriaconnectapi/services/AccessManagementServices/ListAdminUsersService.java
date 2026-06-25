package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.AdminUserListItemResponse;
import co.tz.sheriaconnectapi.model.DTOs.AdminUserSearchInput;
import co.tz.sheriaconnectapi.model.DTOs.PageResponse;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import co.tz.sheriaconnectapi.model.Entities.UserRoleAssignment;
import co.tz.sheriaconnectapi.model.Entities.StaffProfile;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.RoleAssignmentStatus;
import co.tz.sheriaconnectapi.model.Enums.StaffEmploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Set;

@Service
public class ListAdminUsersService
        implements Query<AdminUserSearchInput, PageResponse<AdminUserListItemResponse>> {

    private static final Set<String> SORT_FIELDS =
            Set.of("name", "email", "createdAt", "lastLoginAt", "accountType");

    private final UserRepository userRepository;
    private final AdminUserResponseFactory responseFactory;

    public ListAdminUsersService(
            UserRepository userRepository,
            AdminUserResponseFactory responseFactory
    ) {
        this.userRepository = userRepository;
        this.responseFactory = responseFactory;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<StandardResponse<PageResponse<AdminUserListItemResponse>>> execute(
            AdminUserSearchInput input
    ) {
        String sortField = SORT_FIELDS.contains(input.sortBy()) ? input.sortBy() : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(input.sortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(
                Math.max(input.page(), 0),
                Math.min(Math.max(input.size(), 1), 100),
                Sort.by(direction, sortField)
        );

        Page<AdminUserListItemResponse> page = userRepository
                .findAll(specification(input), pageable)
                .map(user -> responseFactory.create(user, input.includeLinkedIdentity()));

        return ResponseUtil.success(
                PageResponse.from(page),
                "Users retrieved",
                HttpStatus.OK
        );
    }

    private Specification<User> specification(AdminUserSearchInput input) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();

            if (input.capability() != null) {
                if (input.capability() == AccessContext.STAFF) {
                    Subquery<Long> staff = query.subquery(Long.class);
                    var profile = staff.from(StaffProfile.class);
                    staff.select(profile.get("user").get("id"));
                    staff.where(
                            builder.equal(profile.get("user").get("id"), root.get("id")),
                            profile.get("employmentStatus").in(
                                    StaffEmploymentStatus.PENDING,
                                    StaffEmploymentStatus.ACTIVE,
                                    StaffEmploymentStatus.SUSPENDED
                            )
                    );
                    predicates.add(builder.exists(staff));
                } else {
                    Subquery<Long> capability = query.subquery(Long.class);
                    var assignment = capability.from(UserRoleAssignment.class);
                    capability.select(assignment.get("user").get("id"));
                    capability.where(
                            builder.equal(assignment.get("user").get("id"), root.get("id")),
                            builder.equal(assignment.get("context"), input.capability()),
                            builder.equal(assignment.get("status"), RoleAssignmentStatus.ACTIVE),
                            builder.or(
                                    builder.isNull(assignment.get("expiresAt")),
                                    builder.greaterThan(assignment.get("expiresAt"), java.time.Instant.now())
                            )
                    );
                    predicates.add(builder.exists(capability));
                }
            }
            if (input.active() != null) {
                predicates.add(builder.equal(root.get("active"), input.active()));
            }
            if (input.locked() != null) {
                predicates.add(builder.equal(root.get("locked"), input.locked()));
            }
            if (input.search() != null && !input.search().isBlank()) {
                String search = "%" + input.search().trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), search),
                        builder.like(builder.lower(root.get("email")), search)
                ));
            }
            if (input.roleId() != null) {
                predicates.add(builder.equal(
                        root.join("roles", JoinType.LEFT).get("id"),
                        input.roleId()
                ));
                query.distinct(true);
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
