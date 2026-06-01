package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Enums.MatchingRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseMatchRequestRepository extends JpaRepository<CaseMatchRequest, Long> {
    boolean existsByIncidentReportAndProviderProfile(
            IncidentReport incidentReport,
            ProviderProfile providerProfile
    );

    List<CaseMatchRequest> findByIncidentReportOrderByScoreDescCreatedAtAsc(
            IncidentReport incidentReport
    );

    List<CaseMatchRequest> findByIncidentReportAndStatusOrderByCreatedAtAsc(
            IncidentReport incidentReport,
            MatchingRequestStatus status
    );

    @Query("""
            SELECT DISTINCT request FROM CaseMatchRequest request
            JOIN FETCH request.incidentReport
            JOIN FETCH request.providerProfile profile
            LEFT JOIN FETCH profile.specialties
            LEFT JOIN FETCH profile.regions
            LEFT JOIN FETCH profile.languages
            WHERE profile = :providerProfile
            ORDER BY request.createdAt DESC
            """)
    List<CaseMatchRequest> findByProviderProfileOrderByCreatedAtDesc(
            @Param("providerProfile") ProviderProfile providerProfile
    );

    @Query("""
            SELECT DISTINCT request FROM CaseMatchRequest request
            JOIN FETCH request.incidentReport
            JOIN FETCH request.providerProfile profile
            LEFT JOIN FETCH profile.specialties
            LEFT JOIN FETCH profile.regions
            LEFT JOIN FETCH profile.languages
            WHERE request.id = :id
            """)
    java.util.Optional<CaseMatchRequest> findDetailById(@Param("id") Long id);
}
