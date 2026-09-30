package co.tz.sheriaconnectapi.services.IncidentReportServices;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class IncidentReportSubmissionIdempotencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private IncidentReportRepository reportRepository;

    private final List<UUID> submissionIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (UUID submissionId : submissionIds) {
            reportRepository.findBySubmissionId(submissionId)
                    .ifPresent(reportRepository::delete);
        }
    }

    @Test
    void sequentialReplayReturnsOriginalCaseAndTrackingToken() throws Exception {
        UUID submissionId = remember(UUID.randomUUID());
        String payload = payload(submissionId, "Sequential retry details");

        MvcResult first = submit(payload);
        MvcResult replay = submit(payload);

        assertEquals(201, first.getResponse().getStatus());
        assertEquals(200, replay.getResponse().getStatus());
        assertSameCaseAndToken(first, replay);
        assertEquals(1, reportRepository.findAll().stream()
                .filter(report -> submissionId.equals(report.getSubmissionId()))
                .count());
    }

    @Test
    void simultaneousReplayCreatesExactlyOneReport() throws Exception {
        UUID submissionId = remember(UUID.randomUUID());
        String payload = payload(submissionId, "Concurrent retry details");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<MvcResult> first = executor.submit(() -> submitWhenReleased(payload, ready, start));
            Future<MvcResult> second = executor.submit(() -> submitWhenReleased(payload, ready, start));
            ready.await();
            start.countDown();

            MvcResult firstResult = first.get();
            MvcResult secondResult = second.get();
            List<Integer> statuses = new ArrayList<>(List.of(
                    firstResult.getResponse().getStatus(),
                    secondResult.getResponse().getStatus()
            ));
            Collections.sort(statuses);

            assertEquals(List.of(200, 201), statuses);
            assertSameCaseAndToken(firstResult, secondResult);
            assertEquals(1, reportRepository.findAll().stream()
                    .filter(report -> submissionId.equals(report.getSubmissionId()))
                    .count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void reusedSubmissionIdWithChangedPayloadReturnsCodedConflict() throws Exception {
        UUID submissionId = remember(UUID.randomUUID());
        MvcResult first = submit(payload(submissionId, "Original report details"));
        MvcResult conflict = submit(payload(submissionId, "Changed report details"));

        assertEquals(201, first.getResponse().getStatus());
        assertEquals(409, conflict.getResponse().getStatus());
        JsonNode response = objectMapper.readTree(conflict.getResponse().getContentAsString());
        assertEquals("REPORT_SUBMISSION_ID_REUSED", response.path("code").asText());
    }

    private MvcResult submitWhenReleased(
            String payload,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        start.await();
        return submit(payload);
    }

    private MvcResult submit(String payload) throws Exception {
        return mockMvc.perform(post("/incident-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andReturn();
    }

    private void assertSameCaseAndToken(MvcResult first, MvcResult second) throws Exception {
        JsonNode firstBody = responseBody(first);
        JsonNode secondBody = responseBody(second);
        assertEquals(firstBody.path("caseNumber").asText(), secondBody.path("caseNumber").asText());
        assertEquals(firstBody.path("trackingToken").asText(), secondBody.path("trackingToken").asText());
        assertNotNull(firstBody.path("trackingToken").textValue());
    }

    private JsonNode responseBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("body");
    }

    private String payload(UUID submissionId, String description) {
        return """
                {
                  "submissionId": "%s",
                  "anonymityMode": "FULLY_ANONYMOUS",
                  "incidentType": "GENDER_BASED_VIOLENCE",
                  "urgency": "HIGH",
                  "description": "%s",
                  "matchingRequested": true
                }
                """.formatted(submissionId, description);
    }

    private UUID remember(UUID submissionId) {
        submissionIds.add(submissionId);
        return submissionId;
    }
}
