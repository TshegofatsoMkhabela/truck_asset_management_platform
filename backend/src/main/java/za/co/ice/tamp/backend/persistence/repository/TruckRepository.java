package za.co.ice.tamp.backend.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ice.tamp.backend.persistence.entity.Truck;

public interface TruckRepository extends JpaRepository<Truck, UUID> {

    /** Backs FR-04: a Transporter views only their own trucks. */
    List<Truck> findByTransporterId(UUID transporterId);

    List<Truck> findByStatus(String status);
}
