package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.CaseStatusHistory;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseStatusHistoryRepository extends JpaRepository<CaseStatusHistory, Long> {
    List<CaseStatusHistory> findByIncidentReportOrderByCreatedAtAsc(IncidentReport incidentReport);

    Optional<CaseStatusHistory> findFirstByIncidentReportAndToStatusOrderByCreatedAtDesc(
            IncidentReport incidentReport,
            IncidentReportStatus toStatus
    );
}
