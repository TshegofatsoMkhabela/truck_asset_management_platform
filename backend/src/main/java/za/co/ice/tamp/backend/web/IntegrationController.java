package za.co.ice.tamp.backend.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.ice.tamp.backend.integration.MatchingServiceClient;
import za.co.ice.tamp.backend.integration.PingResponse;

/**
 * Exposes the orchestrator to matching-service round trip so it can be exercised
 * over HTTP, which is what makes the end-to-end test a genuine network call rather
 * than an in-process method invocation.
 *
 * <p>Temporary: this endpoint proves the wiring for #5 and is replaced by the real
 * matching endpoint in #13.
 */
@RestController
public class IntegrationController {

    private final MatchingServiceClient matchingServiceClient;

    public IntegrationController(MatchingServiceClient matchingServiceClient) {
        this.matchingServiceClient = matchingServiceClient;
    }

    @GetMapping("/integration/ping")
    public PingResponse pingMatchingService() {
        return matchingServiceClient.ping();
    }
}
