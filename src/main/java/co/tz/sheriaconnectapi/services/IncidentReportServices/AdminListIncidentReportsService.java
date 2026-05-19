package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.AdminIncidentReportSearchInput;
import co.tz.sheriaconnectapi.model.DTOs.IncidentReportSummaryResponse;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminListIncidentReportsService
        implements Query<AdminIncidentReportSearchInput, List<IncidentReportSummaryResponse>> {

    private final IncidentReportRepository incidentReportRepository;
    private final EntityManager entityManager;

    public AdminListIncidentReportsService(
            IncidentReportRepository incidentReportRepository,
            EntityManager entityManager
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.entityManager = entityManager;
    }

    @Override
    public ResponseEntity<StandardResponse<List<IncidentReportSummaryResponse>>> execute(
            AdminIncidentReportSearchInput input
    ) {
        List<IncidentReportSummaryResponse> reports = search(input)
                .stream()
                .map(IncidentReportSummaryResponse::new)
                .toList();

        return ResponseUtil.success(
                reports,
                "Incident reports retrieved successfully",
                HttpStatus.OK
        );
    }

    private List<IncidentReport> search(AdminIncidentReportSearchInput input) {
        if (input.status() == null &&
                input.urgency() == null &&
                input.incidentType() == null &&
                input.anonymityMode() == null &&
                input.matchingRequested() == null &&
                input.createdFrom() == null &&
                input.createdTo() == null) {
            return incidentReportRepository.findAllByOrderByCreatedAtDesc();
        }

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<IncidentReport> criteriaQuery =
                criteriaBuilder.createQuery(IncidentReport.class);
        Root<IncidentReport> report = criteriaQuery.from(IncidentReport.class);

        List<Predicate> predicates = new ArrayList<>();

        if (input.status() != null) {
            predicates.add(criteriaBuilder.equal(report.get("status"), input.status()));
        }

        if (input.urgency() != null) {
            predicates.add(criteriaBuilder.equal(report.get("urgency"), input.urgency()));
        }

        if (input.incidentType() != null) {
            predicates.add(criteriaBuilder.equal(report.get("incidentType"), input.incidentType()));
        }

        if (input.anonymityMode() != null) {
            predicates.add(criteriaBuilder.equal(report.get("anonymityMode"), input.anonymityMode()));
        }

        if (input.matchingRequested() != null) {
            predicates.add(criteriaBuilder.equal(
                    report.get("matchingRequested"),
                    input.matchingRequested()
            ));
        }

        if (input.createdFrom() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    report.get("createdAt"),
                    input.createdFrom()
            ));
        }

        if (input.createdTo() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    report.get("createdAt"),
                    input.createdTo()
            ));
        }

        criteriaQuery
                .select(report)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(criteriaBuilder.desc(report.get("createdAt")));

        return entityManager.createQuery(criteriaQuery).getResultList();
    }
}
