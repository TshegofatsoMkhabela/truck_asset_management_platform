package za.co.ice.tamp.backend.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ice.tamp.backend.persistence.entity.Dispute;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    /** Backs FR-10: the admin console's open-disputes queue. */
    List<Dispute> findByStatus(String status);
}
