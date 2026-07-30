package za.co.ice.tamp.backend.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ice.tamp.backend.persistence.entity.Load;

public interface LoadRepository extends JpaRepository<Load, UUID> {

    /** Backs a Freight Owner viewing only their own postings. */
    List<Load> findByOwnerId(UUID ownerId);

    List<Load> findByOwnerIdAndStatus(UUID ownerId, String status);
}
