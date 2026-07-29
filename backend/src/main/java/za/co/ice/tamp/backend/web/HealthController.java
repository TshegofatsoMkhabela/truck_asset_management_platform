package za.co.ice.tamp.backend.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public liveness probe for monitoring tools and container healthchecks.
 *
 * <p>Deliberately unauthenticated: a probe that requires credentials is useless to
 * the orchestrator that needs to poll it. Kept free of Spring Boot Actuator so the
 * service exposes exactly this one endpoint and nothing else.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "backend"
        );
    }
}
