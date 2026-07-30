package za.co.ice.tamp.backend.integration;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls the Python matching-service over HTTP.
 *
 * <p>Wraps {@link RestClient}, Spring's synchronous HTTP client, rather than exposing
 * it directly, so the rest of the codebase never learns the other service's URL shape.
 *
 * <p>The client is handed a pre-configured {@link RestClient} rather than building its
 * own, which is what lets a test bind a fake HTTP server to the same builder.
 */
@Component
public class MatchingServiceClient {

    private final RestClient restClient;

    public MatchingServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Requests eligible, ranked matches for one load against a set of candidate trucks.
     *
     * <p>Replaces #5's {@code ping()}: this is the real call that method's own Javadoc
     * named as the place the change would land. No error handling or retry here either;
     * that stays a deliberate gap for whichever issue adds resilience across both
     * services, not something to bolt on as a side effect of adding the real call.
     */
    public MatchResponse requestMatches(MatchRequest request) {
        return restClient.post()
                .uri("/match")
                .body(request)
                .retrieve()
                .body(MatchResponse.class);
    }
}
