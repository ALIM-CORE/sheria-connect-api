package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.CaseStatusHistory;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CaseStatusHistoryResponseTest {

    @Test
    void hidesStaffEmailFromCitizenAndProviderResponses() {
        CaseStatusHistory history = history();

        CaseStatusHistoryResponse response = new CaseStatusHistoryResponse(history, false);

        assertNull(response.getChangedByEmail());
        assertEquals("Please provide the district.", response.getNote());
    }

    @Test
    void retainsStaffEmailForAdminResponses() {
        CaseStatusHistory history = history();

        CaseStatusHistoryResponse response = new CaseStatusHistoryResponse(history, true);

        assertEquals("caseworker@sheriaconnect.co.tz", response.getChangedByEmail());
    }

    private CaseStatusHistory history() {
        User staff = new User();
        staff.setEmail("caseworker@sheriaconnect.co.tz");
        CaseStatusHistory history = new CaseStatusHistory();
        history.setChangedByUser(staff);
        history.setToStatus(IncidentReportStatus.NEEDS_INFO);
        history.setNote("Please provide the district.");
        return history;
    }
}
