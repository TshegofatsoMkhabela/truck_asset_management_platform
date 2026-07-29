package za.co.ice.tamp.backend.integration;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls the Python matching-service over HTTP.
 *
 * <p>Wraps {@link RestClient} — Spring's synchronous HTTP client, which replaced
 * {@code RestTemplate} in Spring Boot 3.2 — rather than exposing it directly, so
 * the rest of the codebase never learns the other service's URL shape. When #13
 * replaces {@code /ping} with real matching calls, the change lands here.
 *
 * <p>The client is handed a pre-configured {@link RestClient} rather than building
 * its own, which is what lets a test bind a fake HTTP server to the same builder.
 */
@Component
public class MatchingServiceClient {

    private final RestClient restClient;

    public MatchingServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Round-trips the matching-service {@code /ping} endpoint.
     *
     * <p>No error handling or retry: issue #5 exists to prove the wiring works at
     * all, and a retry here would mask exactly the intermittent failure it is meant
     * to expose. Resilience belongs with the real calls in #13.
     */
    public PingResponse ping() {
        return restClient.get()
                .uri("/ping")
                .retrieve()
                .body(PingResponse.class);
    }
}
