package za.co.ice.tamp.backend.web;

import java.util.UUID;

public class TruckNotFoundException extends RuntimeException {
    public TruckNotFoundException(UUID id) {
        super("Truck not found: " + id);
    }
}
