package za.co.ice.tamp.backend;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import za.co.ice.tamp.backend.persistence.entity.Load;
import za.co.ice.tamp.backend.persistence.entity.Match;
import za.co.ice.tamp.backend.persistence.entity.Truck;
import za.co.ice.tamp.backend.persistence.entity.User;
import za.co.ice.tamp.backend.persistence.repository.LoadRepository;
import za.co.ice.tamp.backend.persistence.repository.MatchRepository;
import za.co.ice.tamp.backend.persistence.repository.TruckRepository;
import za.co.ice.tamp.backend.persistence.repository.UserRepository;

/**
 * Builds the owner, transporter, load, truck and proposed match that every accept, receipt
 * and tracking test needs before it can test anything.
 *
 * <p>Extracted once four test classes needed the identical eleven-line setup. Test source
 * only, so it never ships, and a {@code @Component} rather than a static helper so callers
 * inject one collaborator instead of four repositories they otherwise never touch.
 *
 * <p><strong>Every caller must pass its own unique emails.</strong> The Testcontainers
 * Postgres is shared across the whole test JVM and is never truncated between classes, so a
 * literal address reused across classes fails on {@code users_email_key} in whichever test
 * happens to run second. #13 hit exactly that and lost time to a failure that appeared in an
 * unrelated test class.
 */
@Component
public class MatchFixture {

    private final UserRepository users;
    private final LoadRepository loads;
    private final TruckRepository trucks;
    private final MatchRepository matches;

    public MatchFixture(
            UserRepository users,
            LoadRepository loads,
            TruckRepository trucks,
            MatchRepository matches) {
        this.users = users;
        this.loads = loads;
        this.trucks = trucks;
        this.matches = matches;
    }

    public User owner(String email) {
        return users.save(new User("Owner", email, "hash", "FREIGHT_OWNER"));
    }

    /** A match in its default PROPOSED state, with a real load and truck behind it. */
    public Match proposedMatch(User owner, String transporterEmail) {
        User transporter = users.save(
                new User("Transporter", transporterEmail, "hash", "TRANSPORTER"));
        Load load = loads.save(new Load(owner.getId(), "Johannesburg", "Durban", "GENERAL",
                new BigDecimal("10000.00"), new BigDecimal("20.00"),
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(2)));
        Truck truck = trucks.save(new Truck(transporter.getId(), "FLATBED",
                new BigDecimal("20000.00"), null, "Johannesburg",
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(2)));
        return matches.save(new Match(load.getId(), truck.getId(),
                new BigDecimal("87.50"), List.of("capacity sufficient")));
    }
}
