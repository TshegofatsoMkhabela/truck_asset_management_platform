package za.co.ice.tamp.backend.acceptance;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.ice.tamp.backend.persistence.entity.AuditLog;
import za.co.ice.tamp.backend.persistence.entity.Match;
import za.co.ice.tamp.backend.persistence.entity.MatchStatus;
import za.co.ice.tamp.backend.persistence.entity.Receipt;
import za.co.ice.tamp.backend.persistence.repository.AuditLogRepository;
import za.co.ice.tamp.backend.persistence.repository.MatchRepository;
import za.co.ice.tamp.backend.persistence.repository.ReceiptRepository;
import za.co.ice.tamp.backend.web.MatchNotFoundException;

/**
 * Turns a proposed match into a decided one, recording who decided and when,
 * issuing the receipt an acceptance produces, and writing the audit event that makes the
 * commitment traceable.
 *
 * <p>Kept separate from the HTTP controller for the same reason {@code MatchingCoordinator}
 * is: the decision sequence is the part worth testing without HTTP in the way, and an admin
 * action reversing a decision later would call this directly.
 *
 * <p>{@code actorId} is supplied by the caller rather than read from an authenticated
 * session, exactly as #13 does, because #9 (auth/RBAC) has not merged and there is no
 * security context to read an actor from yet. Recorded in known-limitations.md.
 */
@Service
public class AcceptanceCoordinator {

    private final MatchRepository matches;
    private final ReceiptRepository receipts;
    private final AuditLogRepository auditLogs;

    public AcceptanceCoordinator(
            MatchRepository matches, ReceiptRepository receipts, AuditLogRepository auditLogs) {
        this.matches = matches;
        this.receipts = receipts;
        this.auditLogs = auditLogs;
    }

    /**
     * Transactional so the status change and the receipt commit together. Without it an
     * acceptance whose receipt insert failed would leave a match marked ACCEPTED with no
     * confirmation to retrieve, which is precisely the inconsistency this design rules out.
     *
     * <p>{@code ipAddress} and {@code userAgent} are nullable by design: the brief asks for
     * them "where available", so absence is valid data rather than a placeholder to invent.
     */
    @Transactional
    public Match decide(
            UUID matchId, String decision, UUID actorId, String ipAddress, String userAgent) {
        Match match = matches.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));

        if (!MatchStatus.PROPOSED.equals(match.getStatus())) {
            throw new MatchAlreadyDecidedException(matchId, match.getStatus());
        }

        match.decide(decision, actorId);
        Match saved = matches.save(match);

        if (MatchStatus.ACCEPTED.equals(decision)) {
            receipts.save(new Receipt(matchId, decision, actorId, ipAddress, userAgent));
        }

        auditLogs.save(new AuditLog(
                actorId,
                "MATCH_" + decision,
                "MATCH",
                matchId,
                Map.of("decision", decision)));

        return saved;
    }

    /**
     * Read in its own transaction, which is what makes the database-generated
     * {@code contract_id} visible: Hibernate never re-reads generated defaults into the
     * persistence context that performed the insert, so only a later, separate read sees it.
     */
    @Transactional(readOnly = true)
    public Receipt receiptFor(UUID matchId) {
        return receipts.findByMatchId(matchId)
                .orElseThrow(() -> new ReceiptNotFoundException(matchId));
    }
}
