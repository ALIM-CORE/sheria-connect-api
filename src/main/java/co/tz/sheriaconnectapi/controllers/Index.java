package co.tz.sheriaconnectapi.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;

@RestController
public class Index {
    private final JdbcTemplate jdbcTemplate;
    private final String revision;

    public Index(JdbcTemplate jdbcTemplate, @Value("${app.release.revision}") String revision) {
        this.jdbcTemplate = jdbcTemplate;
        this.revision = revision;
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> index() {
        return health();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(healthResponse("UP"));
        } catch (DataAccessException exception) {
            return ResponseEntity.status(503).body(healthResponse("DOWN"));
        }
    }

    private Map<String, String> healthResponse(String status) {
        return Map.of(
                "status", status,
                "service", "sheria-connect-api",
                "revision", revision,
                "timestamp", Instant.now().toString()
        );
    }
}
