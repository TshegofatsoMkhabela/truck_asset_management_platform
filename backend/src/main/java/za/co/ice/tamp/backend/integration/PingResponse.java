package za.co.ice.tamp.backend.integration;

/**
 * The matching-service {@code /ping} payload.
 *
 * <p>A record rather than a map so a change to the other service's response shape
 * fails at deserialization with a named field, instead of surfacing later as a null
 * lookup somewhere further from the cause.
 */
public record PingResponse(String service, boolean pong) {
}
