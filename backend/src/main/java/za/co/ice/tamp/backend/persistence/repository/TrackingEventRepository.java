package za.co.ice.tamp.backend.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ice.tamp.backend.persistence.entity.TrackingEvent;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, UUID> {

    /** Backs FR-08: the trip progress view reads events oldest-first. */
    List<TrackingEvent> findByMatchIdOrderByOccurredAtAsc(UUID matchId);

    /** The match's current trip stage, to reject a new status that moves backwards. */
    Optional<TrackingEvent> findFirstByMatchIdAndStatusIsNotNullOrderByOccurredAtDesc(UUID matchId);
}
