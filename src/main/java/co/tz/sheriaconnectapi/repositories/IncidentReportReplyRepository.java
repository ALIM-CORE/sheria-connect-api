package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.CaseStatusHistory;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.IncidentReportReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentReportReplyRepository extends JpaRepository<IncidentReportReply, Long> {
    List<IncidentReportReply> findByIncidentReportOrderByCreatedAtAsc(IncidentReport incidentReport);

    boolean existsByNeedsInfoHistory(CaseStatusHistory needsInfoHistory);
}
