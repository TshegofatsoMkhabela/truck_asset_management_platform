package za.co.ice.tamp.backend.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Builds the {@link RestClient} pointed at matching-service.
 *
 * <p>The base URL comes from the {@code MATCHING_SERVICE_URL} environment variable
 * so Docker Compose in #8 can repoint it at a container hostname without a code
 * change. The localhost default keeps a plain {@code mvn spring-boot:run} working
 * for anyone following the README.
 */
@Configuration
public class MatchingServiceConfig {

    @Bean
    public RestClient matchingServiceRestClient(
            @Value("${matching-service.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
