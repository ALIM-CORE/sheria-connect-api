package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.CaseMessage;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AnonymityMode;
import co.tz.sheriaconnectapi.model.Enums.CaseMessageSenderRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CaseMessageResponseTest {

    @Test
    void namedCitizenUsesSubmittedContactNameWithoutAccountIdentity() {
        CaseMessageResponse response = citizenResponse(
                AnonymityMode.NAMED,
                "Visible Name",
                null
        );

        assertNull(response.senderUserId());
        assertEquals("Visible Name", response.senderName());
    }

    @Test
    void pseudonymousCitizenUsesPseudonymWithoutAccountIdentity() {
        CaseMessageResponse response = citizenResponse(
                AnonymityMode.PSEUDONYMOUS,
                "Ignored Contact Name",
                "Hopeful Voice"
        );

        assertNull(response.senderUserId());
        assertEquals("Hopeful Voice", response.senderName());
    }

    @Test
    void fullyAnonymousCitizenExposesNoIdentity() {
        CaseMessageResponse response = citizenResponse(
                AnonymityMode.FULLY_ANONYMOUS,
                "Ignored Contact Name",
                "Ignored Pseudonym"
        );

        assertNull(response.senderUserId());
        assertNull(response.senderName());
    }

    @Test
    void providerIdentityRemainsVisibleToCitizen() {
        IncidentReport report = report(AnonymityMode.FULLY_ANONYMOUS, null, null);
        User provider = new User();
        provider.setId(12L);
        provider.setName("Amina Advocate");

        CaseMessage message = message(report, provider, CaseMessageSenderRole.PROVIDER);
        CaseMessageResponse response = new CaseMessageResponse(message);

        assertEquals(12L, response.senderUserId());
        assertEquals("Amina Advocate", response.senderName());
    }

    private CaseMessageResponse citizenResponse(
            AnonymityMode anonymityMode,
            String contactName,
            String pseudonym
    ) {
        IncidentReport report = report(anonymityMode, contactName, pseudonym);
        User account = new User();
        account.setId(9L);
        account.setName("Private Account Name");
        return new CaseMessageResponse(
                message(report, account, CaseMessageSenderRole.CITIZEN)
        );
    }

    private IncidentReport report(
            AnonymityMode anonymityMode,
            String contactName,
            String pseudonym
    ) {
        IncidentReport report = new IncidentReport();
        report.setCaseNumber("SC-2609-PRIVACY");
        report.setAnonymityMode(anonymityMode);
        report.setContactName(contactName);
        report.setPseudonym(pseudonym);
        return report;
    }

    private CaseMessage message(
            IncidentReport report,
            User sender,
            CaseMessageSenderRole senderRole
    ) {
        CaseMessage message = new CaseMessage();
        message.setIncidentReport(report);
        message.setSenderUser(sender);
        message.setSenderRole(senderRole);
        message.setBody("Test message");
        return message;
    }
}
