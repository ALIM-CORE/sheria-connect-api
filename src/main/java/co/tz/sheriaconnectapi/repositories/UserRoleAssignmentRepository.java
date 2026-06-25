package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Entities.UserRoleAssignment;
import co.tz.sheriaconnectapi.model.Entities.StaffInvitation;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.RoleAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, Long> {
    @Query("""
            SELECT DISTINCT assignment FROM UserRoleAssignment assignment
            JOIN FETCH assignment.role role
            LEFT JOIN FETCH role.authorities
            WHERE assignment.user = :user
              AND assignment.context = :context
              AND assignment.status = 'ACTIVE'
              AND (assignment.expiresAt IS NULL OR assignment.expiresAt > :now)
            """)
    List<UserRoleAssignment> findActive(
            @Param("user") User user,
            @Param("context") AccessContext context,
            @Param("now") Instant now
    );

    List<UserRoleAssignment> findAllByUserOrderByCreatedAtDesc(User user);

    List<UserRoleAssignment> findAllByUserAndContextOrderByCreatedAtDesc(
            User user,
            AccessContext context
    );

    List<UserRoleAssignment> findAllByStaffInvitation(StaffInvitation staffInvitation);

    Optional<UserRoleAssignment> findFirstByUserAndRole_IdAndContextAndStatusIn(
            User user,
            Long roleId,
            AccessContext context,
            Collection<RoleAssignmentStatus> statuses
    );

    Optional<UserRoleAssignment> findFirstByUserAndRole_IdAndContextOrderByCreatedAtDesc(
            User user,
            Long roleId,
            AccessContext context
    );

    long countByRole_IdAndContextAndStatus(Long roleId, AccessContext context, RoleAssignmentStatus status);

    boolean existsByRole_Id(Long roleId);

    @Query("""
            SELECT COUNT(DISTINCT assignment.user.id)
            FROM UserRoleAssignment assignment
            WHERE assignment.role.id = :roleId
              AND assignment.status = 'ACTIVE'
              AND (assignment.expiresAt IS NULL OR assignment.expiresAt > :now)
            """)
    long countActiveUsersByRole(
            @Param("roleId") Long roleId,
            @Param("now") Instant now
    );

    @Query("""
            SELECT COUNT(DISTINCT assignment.user.id)
            FROM UserRoleAssignment assignment
            WHERE assignment.role.id = :roleId
              AND assignment.context = :context
              AND assignment.status = 'ACTIVE'
              AND (assignment.expiresAt IS NULL OR assignment.expiresAt > :now)
              AND assignment.user.active = true
              AND assignment.user.locked = false
            """)
    long countUsableUsersByRoleAndContext(
            @Param("roleId") Long roleId,
            @Param("context") AccessContext context,
            @Param("now") Instant now
    );

    @Query("""
            SELECT COUNT(DISTINCT assignment.user.id)
            FROM UserRoleAssignment assignment
            WHERE assignment.context = :context
              AND assignment.status = :status
              AND (assignment.expiresAt IS NULL OR assignment.expiresAt > :now)
            """)
    long countDistinctUsers(
            @Param("context") AccessContext context,
            @Param("status") RoleAssignmentStatus status,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
            UPDATE UserRoleAssignment assignment
               SET assignment.status = 'EXPIRED',
                   assignment.updatedAt = :now
             WHERE assignment.status = 'ACTIVE'
               AND assignment.expiresAt IS NOT NULL
               AND assignment.expiresAt <= :now
            """)
    int expireAssignments(@Param("now") Instant now);
}
