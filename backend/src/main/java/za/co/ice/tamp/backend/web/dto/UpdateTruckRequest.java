package za.co.ice.tamp.backend.web.dto;

/** Partial update: only the fields a caller might want to change after posting. */
public record UpdateTruckRequest(
        /** New status: "available", "in-use", "maintenance", etc. */
        String status
) {
}
