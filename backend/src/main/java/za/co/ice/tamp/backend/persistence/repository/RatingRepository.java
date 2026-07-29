package za.co.ice.tamp.backend.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ice.tamp.backend.persistence.entity.Rating;

public interface RatingRepository extends JpaRepository<Rating, UUID> {

    /** Backs FR-11: platform metrics are derived by query, never stored (ADR-2). */
    List<Rating> findByRateeId(UUID rateeId);
}
