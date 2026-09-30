package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.CaseStatusHistory;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.IncidentReportReply;
import co.tz.sheriaconnectapi.model.Entities.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProviderAcceptedCaseDetailResponseTest {

    @Test
    void includesReporterRepliesWithoutExposingStaffEmail() {
        IncidentReport report = new IncidentReport();
        report.setCaseNumber("SC-2609-ABC234");

        User staff = new User();
        staff.setEmail("staff@sheriaconnect.co.tz");
        CaseStatusHistory needsInfo = new CaseStatusHistory();
        needsInfo.setId(71L);
        needsInfo.setChangedByUser(staff);
        needsInfo.setCreatedAt(Instant.parse("2026-09-29T10:00:00Z"));

        IncidentReportReply reply = new IncidentReportReply();
        reply.setId(9L);
        reply.setIncidentReport(report);
        reply.setNeedsInfoHistory(needsInfo);
        reply.setBody("The incident happened near the district office.");
        reply.setCreatedAt(Instant.parse("2026-09-29T11:00:00Z"));

        ProviderAcceptedCaseDetailResponse response =
                new ProviderAcceptedCaseDetailResponse(
                        report,
                        List.of(),
                        List.of(needsInfo),
                        List.of(reply)
                );

        assertNull(response.getStatusHistory().getFirst().getChangedByEmail());
        assertEquals(
                "The incident happened near the district office.",
                response.getReporterReplies().getFirst().body()
        );
    }
}
