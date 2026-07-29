package za.co.ice.tamp.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Maps {@code users} (db/migrations/V01__users.sql).
 *
 * <p>{@code id}, {@code created_at} and {@code updated_at} are database-generated
 * ({@code uuidv7()}, {@code now()}, and the trigger from V10) and marked
 * {@code insertable = false, updatable = false} so the database stays their only author;
 * an entity-assigned value here would silently discard the time-ordering the key type
 * was chosen for (ADR-2).
 *
 * <p>{@code email} is {@code CITEXT} in the database but needs no special Java mapping:
 * PostgreSQL accepts an implicit assignment cast from {@code varchar} to {@code citext},
 * so a plain {@code String} round-trips correctly and the case-insensitive uniqueness
 * stays enforced by the index, not by this entity.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(insertable = false, updatable = false)
    private UUID id;

    @Column(name = "full_name")
    private String fullName;

    /**
     * {@code columnDefinition} is required: Hibernate's schema validator compares column type
     * codes strictly, and it sees {@code citext} as {@code Types#OTHER} against the
     * {@code varchar}/{@code Types#VARCHAR} it expects for a plain String, so validation fails
     * even though reads and writes work correctly at runtime. This was caught by actually
     * running the validator, not asserted correctly up front: an earlier version of this class
     * claimed CITEXT "needs no special mapping," which is true for JDBC binding and false for
     * validation.
     */
    @Column(columnDefinition = "citext")
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    private String role;

    @Column(name = "compliance_status", insertable = false)
    private String complianceStatus;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected User() {
    }

    public User(String fullName, String email, String passwordHash, String role) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getComplianceStatus() {
        return complianceStatus;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
