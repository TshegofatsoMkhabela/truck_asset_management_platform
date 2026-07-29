package brokenentity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A deliberately mismatched entity, used only by {@code SchemaValidationTest} to prove
 * {@code ddl-auto: validate} actually rejects drift.
 *
 * <p>Lives in a top-level package outside {@code za.co.ice.tamp.backend}, not alongside the
 * real entities, because Spring Boot's default component scan starts at
 * {@code BackendApplication}'s package and walks downward. A first attempt nested this class
 * inside the test itself, still under {@code za.co.ice.tamp.backend.persistence}, and it was
 * picked up by every other test's full application context, failing all of them on an entity
 * meant to break exactly one test.
 */
@Entity
@Table(name = "users")
public class BrokenEntity {

    @Id
    UUID id;

    @Column(name = "this_column_does_not_exist")
    String bogus;
}
