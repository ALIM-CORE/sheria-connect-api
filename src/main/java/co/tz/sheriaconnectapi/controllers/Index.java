package co.tz.sheriaconnectapi.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class Index {

    @GetMapping("/")
    public Map<String, String> index() {
        return healthyResponse();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return healthyResponse();
    }

    private Map<String, String> healthyResponse() {
        return Map.of(
                "status", "UP",
                "service", "sheria-connect-api",
                "timestamp", Instant.now().toString()
        );
    }
}
