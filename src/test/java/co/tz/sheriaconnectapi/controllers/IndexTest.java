package co.tz.sheriaconnectapi.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IndexTest {
    @Test
    void readinessIncludesTheRunningRevisionAndChecksTheDatabase() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        var response = new Index(jdbc, "tested-revision").health();
        assertEquals(200, response.getStatusCode().value());
        assertEquals("tested-revision", response.getBody().get("revision"));
        assertEquals("UP", response.getBody().get("status"));
    }

    @Test
    void databaseFailureIsUnavailableWithoutLeakingConnectionDetails() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new DataAccessResourceFailureException("private connection details"));
        var response = new Index(jdbc, "tested-revision").health();
        assertEquals(503, response.getStatusCode().value());
        assertEquals("DOWN", response.getBody().get("status"));
        assertEquals(4, response.getBody().size());
    }
}
