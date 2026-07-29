package za.co.ice.tamp.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Unit-level proof that {@link MatchingServiceClient} calls the right URL with the
 * right verb and parses the response — without a real matching-service running.
 *
 * <p>Runs inside the normal backend test suite (unlike the real end-to-end test in
 * {@code CrossServiceIntegrationE2ETest}), so it also keeps the new client class
 * inside #2's 80% coverage gate: the E2E test is tagged out of that suite to avoid
 * coupling the backend job to a live Python process, which would otherwise leave
 * this class uncovered in that job.
 *
 * <p>{@link MockRestServiceServer} intercepts at the HTTP client layer, so it
 * defends against a wrong path or verb that a real call would only surface as an
 * opaque 404 — the assertion here names exactly what request was expected.
 */
class MatchingServiceClientTest {

    @Test
    void callsMatchingServicePingEndpoint() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://matching-service.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("http://matching-service.test/ping"))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        "{\"service\":\"matching-service\",\"pong\":true}",
                        APPLICATION_JSON));

        MatchingServiceClient client = new MatchingServiceClient(builder.build());

        PingResponse response = client.ping();

        assertThat(response.service()).isEqualTo("matching-service");
        assertThat(response.pong()).isTrue();
        server.verify();
    }
}
