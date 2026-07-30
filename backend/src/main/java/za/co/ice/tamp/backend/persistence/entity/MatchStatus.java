package za.co.ice.tamp.backend.persistence.entity;

/**
 * The three values {@code matches.status} accepts, mirroring the CHECK constraint in
 * {@code db/migrations/V04__matches.sql}.
 *
 * <p>Constants rather than an enum because {@link Match} maps the column as a {@code String},
 * and because these are needed inside annotation attributes ({@code DecisionRequest}'s
 * validation pattern), which only accept compile-time constants.
 *
 * <p>They live here, beside the entity that owns the column, because two packages must agree
 * on them: acceptance writes {@code ACCEPTED}, and tracking refuses to record a trip unless it
 * reads back that exact string. Declared separately in each, a change to one would leave
 * tracking silently rejecting every accepted match, with nothing to catch the drift.
 */
public final class MatchStatus {

    public static final String PROPOSED = "PROPOSED";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String REJECTED = "REJECTED";

    private MatchStatus() {
    }
}
