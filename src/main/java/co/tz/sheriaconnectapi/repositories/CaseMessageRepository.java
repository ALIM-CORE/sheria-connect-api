package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Entities.CaseMessage;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseMessageRepository extends JpaRepository<CaseMessage, Long> {
    List<CaseMessage> findByIncidentReportOrderByCreatedAtAsc(IncidentReport incidentReport);

    List<CaseMessage> findByCaseMatchRequestOrderByCreatedAtAsc(CaseMatchRequest caseMatchRequest);

    List<CaseMessage> findByIncidentReportAndIdGreaterThanOrderByCreatedAtAsc(
            IncidentReport incidentReport,
            Long id
    );

    List<CaseMessage> findByCaseMatchRequestAndIdGreaterThanOrderByCreatedAtAsc(
            CaseMatchRequest caseMatchRequest,
            Long id
    );
}
