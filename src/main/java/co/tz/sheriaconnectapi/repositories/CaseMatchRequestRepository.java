package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
