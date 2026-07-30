package za.co.ice.tamp.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Unit-level proof that {@link MatchingServiceClient} sends the right payload shape
 * to {@code /match} and parses the response, without a real matching-service running.
 *
 * <p>{@link MockRestServiceServer} intercepts at the HTTP client layer, so it defends
 * against exactly the failure that bit the request DTOs while writing them: Jackson
 * serialising Java's camelCase field names when matching-service expects snake_case.
 * A test that only checked the response would miss a request FastAPI silently
 * accepts but ignores every field of.
 */
class MatchingServiceClientTest {

    @Test
    void requestsMatchesWithSnakeCaseFieldNamesAndParsesTheResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://matching-service.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        LoadInput load = new LoadInput(
                "load-1", "Johannesburg", "GENERAL", 10000.0,
                OffsetDateTime.parse("2026-08-03T06:00:00Z"),
                OffsetDateTime.parse("2026-08-05T06:00:00Z"));
        TruckInput truck = new TruckInput(
                "truck-1", "Johannesburg", "FLATBED", 20000.0,
                OffsetDateTime.parse("2026-08-03T06:00:00Z"),
                OffsetDateTime.parse("2026-08-05T06:00:00Z"));

        server.expect(requestTo("http://matching-service.test/match"))
                .andExpect(method(POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"origin_city\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"weight_kg\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"vehicle_type\"")))
                .andRespond(withSuccess(
                        "{\"matches\":[{\"truck_id\":\"truck-1\",\"score\":95.0,"
                                + "\"reasons\":[\"capacity sufficient\"]}]}",
                        APPLICATION_JSON));

        MatchingServiceClient client = new MatchingServiceClient(builder.build());

        MatchResponse response = client.requestMatches(new MatchRequest(load, List.of(truck)));

        assertThat(response.matches()).hasSize(1);
        assertThat(response.matches().get(0).truckId()).isEqualTo("truck-1");
        assertThat(response.matches().get(0).score()).isEqualTo(95.0);
        assertThat(response.matches().get(0).reasons()).containsExactly("capacity sufficient");
        server.verify();
    }
}
