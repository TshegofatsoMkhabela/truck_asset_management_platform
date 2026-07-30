package za.co.ice.tamp.backend.web.dto;

/**
 * Basic platform counts. Simple totals by design: the brief asks for basic metrics, and
 * dashboards are a front-end concern outside this track.
 */
public record MetricsResponse(long users, long loads, long trucks, long matches) {
}
