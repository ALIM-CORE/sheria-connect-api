package co.tz.sheriaconnectapi.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GuestCaseMessagingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousGetReachesMessageDomainAuthorization() throws Exception {
        mockMvc.perform(get("/incident-reports/SC-NOT-FOUND/messages")
                        .header("X-Case-Tracking-Token", "tracking-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousPostReachesMessageDomainAuthorization() throws Exception {
        mockMvc.perform(post("/incident-reports/SC-NOT-FOUND/messages")
                        .header("X-Case-Tracking-Token", "tracking-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Test message\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousInformationReplyReachesCaseDomainAuthorization() throws Exception {
        mockMvc.perform(post("/incident-reports/SC-2609-ABC234/replies")
                        .header("X-Case-Tracking-Token", "tracking-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Additional information\"}"))
                .andExpect(status().isNotFound());
    }
}
