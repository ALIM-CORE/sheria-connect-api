package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.CaseStatusHistory;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseStatusHistoryRepository extends JpaRepository<CaseStatusHistory, Long> {
    List<CaseStatusHistory> findByIncidentReportOrderByCreatedAtAsc(IncidentReport incidentReport);
}
