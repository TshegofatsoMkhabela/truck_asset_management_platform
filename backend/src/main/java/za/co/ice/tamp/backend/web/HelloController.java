package za.co.ice.tamp.backend.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Minimal liveness response proving the service starts and serves traffic.
 * A dedicated {@code /health} endpoint replaces this as the monitoring surface.
 */
@RestController
public class HelloController {

    @GetMapping("/")
    public Map<String, String> hello() {
        return Map.of(
                "service", "backend",
                "status", "ok",
                "message", "Hello from TAMP backend"
        );
    }
}
