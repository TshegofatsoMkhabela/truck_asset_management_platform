package za.co.ice.tamp.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

public record DisputeRequest(
        @NotBlank String description
) {
}
