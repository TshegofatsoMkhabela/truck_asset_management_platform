package za.co.ice.tamp.backend.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ice.tamp.backend.persistence.entity.Match;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    List<Match> findByLoadId(UUID loadId);

    List<Match> findByTruckId(UUID truckId);
}
