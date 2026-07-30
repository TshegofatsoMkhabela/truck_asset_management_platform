package za.co.ice.tamp.backend.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ice.tamp.backend.persistence.entity.TrackingEvent;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, UUID> {

    /** Backs the trip progress view, which reads events oldest-first. */
    List<TrackingEvent> findByMatchIdOrderByOccurredAtAsc(UUID matchId);
}
