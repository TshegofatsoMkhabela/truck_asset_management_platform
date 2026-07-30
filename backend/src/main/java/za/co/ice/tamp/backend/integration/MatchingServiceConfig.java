package za.co.ice.tamp.backend.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds the {@link RestClient} pointed at matching-service.
 *
 * <p>The base URL comes from the {@code MATCHING_SERVICE_URL} environment variable
 * so Docker Compose in #8 can repoint it at a container hostname without a code
 * change. The localhost default keeps a plain {@code mvn spring-boot:run} working
 * for anyone following the README.
 *
 * <p>Takes Spring Boot's auto-configured {@link RestClient.Builder} as a
 * constructor parameter rather than calling the static {@code RestClient.builder()}
 * factory, so the same Jackson configuration the rest of the app uses is applied
 * consistently.
 *
 * <p>Explicitly uses {@link SimpleClientHttpRequestFactory} (the classic
 * {@code HttpURLConnection}-based client) rather than the JDK's default
 * {@code java.net.http.HttpClient}, which Spring picks automatically when no
 * factory is specified and which is on the classpath here. The JDK client sends
 * an {@code Expect: 100-continue} header for POST requests with a body; uvicorn
 * (matching-service's ASGI server) does not respond to that negotiation, so the
 * client gave up waiting and the request body never actually reached the server,
 * which then reported the whole body as missing, "{"detail":[{"msg":"Field
 * required"...}]}", even though request-level logging confirmed Spring had built
 * and attempted to write a correct body. Discovered only once a real network call
 * carried a request body at all, since {@code /ping} (#5) was a GET with none.
 */
@Configuration
public class MatchingServiceConfig {

    @Bean
    public RestClient matchingServiceRestClient(
            RestClient.Builder builder, @Value("${matching-service.base-url}") String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
    }
}
