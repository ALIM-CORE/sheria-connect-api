package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.StaffProfile;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.StaffEmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {
    Optional<StaffProfile> findByUser(User user);

    long countByEmploymentStatus(StaffEmploymentStatus status);

    @Query("""
            SELECT profile.user.id
            FROM StaffProfile profile
            WHERE profile.employmentStatus IN :statuses
              AND profile.expiresAt IS NOT NULL
              AND profile.expiresAt <= :now
            """)
    List<Long> findExpiredUserIds(
            @Param("statuses") java.util.Collection<StaffEmploymentStatus> statuses,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
            UPDATE StaffProfile profile
               SET profile.employmentStatus = 'EXPIRED',
                   profile.updatedAt = :now
             WHERE profile.employmentStatus IN :statuses
               AND profile.expiresAt IS NOT NULL
               AND profile.expiresAt <= :now
            """)
    int expireProfiles(
            @Param("statuses") java.util.Collection<StaffEmploymentStatus> statuses,
            @Param("now") Instant now
    );
}
