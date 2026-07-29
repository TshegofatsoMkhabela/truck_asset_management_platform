package za.co.ice.tamp.backend.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import za.co.ice.tamp.backend.persistence.JpaTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * The issue #5 deliverable: proves the Java orchestrator reaches the Python
 * matching-service over a real network call and returns what it got back.
 *
 * <p><strong>Requires a running matching-service</strong> on
 * {@code MATCHING_SERVICE_URL} (default {@code http://localhost:8000}). That is why
 * it is tagged {@code e2e} and excluded from the default suite — see the surefire
 * config in {@code pom.xml}. Run it with {@code mvn verify -Pe2e}.
 *
 * <p>Nothing here is mocked. The orchestrator runs on a real port, the request goes
 * out over HTTP, and matching-service answers on its own port in its own process.
 * A mock at any layer would prove only that the mock works — which is precisely the
 * assumption this issue exists to stop the project resting on.
 *
 * <p>The assertion checks {@code service} equals "matching-service" rather than just
 * checking for a 200. A 200 alone would still pass if the orchestrator fabricated a
 * response without ever making the call; asserting on the *other* service's name is
 * what proves the hop actually happened.
 *
 * <p>Extends {@code JpaTestBase} rather than a bare {@code @SpringBootTest}, same as
 * {@code HelloControllerTest}/{@code HealthControllerTest}: once the application had a real
 * datasource (#6), every full-context test needs one to boot, and this one is no exception
 * even though it never touches persistence itself. {@code webEnvironment = RANDOM_PORT} is
 * redeclared here because this test needs a real embedded server for {@code TestRestTemplate}
 * to call, which {@code JpaTestBase}'s own {@code @SpringBootTest} does not request.
 */
@Tag("e2e")
@SpringBootTest(webEnvironment = RANDOM_PORT)
class CrossServiceIntegrationE2ETest extends JpaTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void orchestratorReachesMatchingServiceOverTheNetwork() {
        ResponseEntity<PingResponse> response =
                restTemplate.getForEntity("/integration/ping", PingResponse.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().service()).isEqualTo("matching-service");
        assertThat(response.getBody().pong()).isTrue();
    }
}
