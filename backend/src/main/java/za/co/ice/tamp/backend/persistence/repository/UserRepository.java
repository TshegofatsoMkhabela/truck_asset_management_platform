package za.co.ice.tamp.backend.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.ice.tamp.backend.persistence.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Case-insensitive by explicit {@code lower()} comparison, not by relying on the
     * {@code CITEXT} column type alone: PostgreSQL's operator resolution for
     * {@code citext_column = varchar_param} does not reliably pick the case-insensitive
     * operator once the JDBC driver types the parameter as {@code VARCHAR}, which is what a
     * derived {@code findByEmail} query would send. The database's unique index on {@code email}
     * still enforces case-insensitive uniqueness for writes regardless of this; this query only
     * governs reads, and states the comparison explicitly so it does not depend on how a given
     * driver happens to type its parameters.
     */
    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmail(@Param("email") String email);
}
