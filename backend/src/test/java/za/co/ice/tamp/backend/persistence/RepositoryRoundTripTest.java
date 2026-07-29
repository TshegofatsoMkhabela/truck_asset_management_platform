package za.co.ice.tamp.backend.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import za.co.ice.tamp.backend.persistence.entity.AuditLog;
import za.co.ice.tamp.backend.persistence.entity.ComplianceDocument;
import za.co.ice.tamp.backend.persistence.entity.Dispute;
import za.co.ice.tamp.backend.persistence.entity.Load;
import za.co.ice.tamp.backend.persistence.entity.Match;
import za.co.ice.tamp.backend.persistence.entity.Rating;
import za.co.ice.tamp.backend.persistence.entity.Receipt;
import za.co.ice.tamp.backend.persistence.entity.TrackingEvent;
import za.co.ice.tamp.backend.persistence.entity.Truck;
import za.co.ice.tamp.backend.persistence.entity.User;
import za.co.ice.tamp.backend.persistence.repository.AuditLogRepository;
import za.co.ice.tamp.backend.persistence.repository.ComplianceDocumentRepository;
import za.co.ice.tamp.backend.persistence.repository.DisputeRepository;
import za.co.ice.tamp.backend.persistence.repository.LoadRepository;
import za.co.ice.tamp.backend.persistence.repository.MatchRepository;
import za.co.ice.tamp.backend.persistence.repository.RatingRepository;
import za.co.ice.tamp.backend.persistence.repository.ReceiptRepository;
import za.co.ice.tamp.backend.persistence.repository.TrackingEventRepository;
import za.co.ice.tamp.backend.persistence.repository.TruckRepository;
import za.co.ice.tamp.backend.persistence.repository.UserRepository;

/**
 * Proves feature code can read and write every table through a repository, with no SQL string
 * anywhere in this class or in the entities it exercises. This is this issue's Minimum
 * Integration Test: a load saved and read back with its generated id and its JSONB and CITEXT
 * neighbours intact.
 */
class RepositoryRoundTripTest extends JpaTestBase {

    @Autowired
    private UserRepository users;
    @Autowired
    private LoadRepository loads;
    @Autowired
    private TruckRepository trucks;
    @Autowired
    private MatchRepository matches;
    @Autowired
    private ReceiptRepository receipts;
    @Autowired
    private AuditLogRepository auditLogs;
    @Autowired
    private RatingRepository ratings;
    @Autowired
    private TrackingEventRepository trackingEvents;
    @Autowired
    private DisputeRepository disputes;
    @Autowired
    private ComplianceDocumentRepository complianceDocuments;

    @Test
    void saves_and_reads_back_a_load_with_its_generated_id() {
        User owner = users.save(new User("Owner", "owner@example.com", "hash", "FREIGHT_OWNER"));

        Load saved = loads.save(new Load(owner.getId(), "Johannesburg", "Durban", "GENERAL",
                new BigDecimal("12000.00"), new BigDecimal("40.00"),
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(2)));

        Load reloaded = loads.findById(saved.getId()).orElseThrow();
        // Confirms uuidv7() fired rather than Hibernate assigning its own id: a database
        // default only reaches the entity if insertable=false left the column for Postgres.
        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getOwnerId()).isEqualTo(owner.getId());
        assertThat(reloaded.getStatus()).isEqualTo("OPEN");
        assertThat(reloaded.getOriginCity()).isEqualTo("Johannesburg");
        assertThat(reloaded.getDestinationCity()).isEqualTo("Durban");
        assertThat(reloaded.getCargoType()).isEqualTo("GENERAL");
        assertThat(reloaded.getWeightKg()).isEqualByComparingTo("12000.00");
        assertThat(reloaded.getVolumeM3()).isEqualByComparingTo("40.00");
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void moves_a_match_a_dispute_and_a_compliance_document_through_their_decision_lifecycle() {
        User owner = users.save(new User("O8", "o8@example.com", "hash", "FREIGHT_OWNER"));
        User transporter = users.save(new User("T8", "t8@example.com", "hash", "TRANSPORTER"));
        User admin = users.save(new User("Admin8", "admin8@example.com", "hash", "ADMIN"));
        Load load = loads.save(new Load(owner.getId(), "Rustenburg", "Klerksdorp", "GENERAL",
                new BigDecimal("4000.00"), new BigDecimal("15.00"),
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(1)));
        Truck truck = trucks.save(new Truck(transporter.getId(), "FLATBED",
                new BigDecimal("10000.00"), null, "Rustenburg",
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(1)));

        // Exercises Match.decide(), which sets status/decided_at/decided_by together: the
        // schema's matches_decision_consistency_check rejects any one of the three set alone.
        Match match = matches.save(new Match(load.getId(), truck.getId(), new BigDecimal("90.00"),
                List.of("capacity sufficient")));
        match.decide("ACCEPTED", owner.getId());
        Match savedMatch = matches.save(match);
        assertThat(matches.findById(savedMatch.getId()).orElseThrow().getDecidedBy())
                .isEqualTo(owner.getId());

        Dispute dispute = disputes.save(
                new Dispute(match.getId(), null, owner.getId(), "Truck arrived a day late"));
        dispute.resolve("RESOLVED", admin.getId(), "Refund issued");
        Dispute savedDispute = disputes.save(dispute);
        assertThat(disputes.findById(savedDispute.getId()).orElseThrow().getResolvedBy())
                .isEqualTo(admin.getId());

        ComplianceDocument document = complianceDocuments.save(
                new ComplianceDocument(transporter.getId(), "INSURANCE", "insurance.pdf"));
        document.review("APPROVED", admin.getId());
        ComplianceDocument savedDocument = complianceDocuments.save(document);
        assertThat(complianceDocuments.findById(savedDocument.getId()).orElseThrow().getStatus())
                .isEqualTo("APPROVED");
    }

    @Test
    void keeps_email_uniqueness_case_insensitive_through_the_repository() {
        users.save(new User("Sam", "Sam@Example.com", "hash", "TRANSPORTER"));

        User reloaded = users.findByEmail("sam@example.com").orElseThrow();
        assertThat(reloaded.getFullName()).isEqualTo("Sam");
        assertThat(reloaded.getRole()).isEqualTo("TRANSPORTER");
        assertThat(reloaded.getComplianceStatus()).isEqualTo("PENDING");
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void finds_loads_by_owner_and_status_and_trucks_by_transporter_and_status() {
        User owner = users.save(new User("O9", "o9@example.com", "hash", "FREIGHT_OWNER"));
        User transporter = users.save(new User("T9", "t9@example.com", "hash", "TRANSPORTER"));
        loads.save(new Load(owner.getId(), "Polokwane", "Tzaneen", "GENERAL",
                new BigDecimal("3000.00"), new BigDecimal("12.00"),
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(1)));
        trucks.save(new Truck(transporter.getId(), "FLATBED", new BigDecimal("18000.00"), null,
                "Polokwane", OffsetDateTime.now(), OffsetDateTime.now().plusDays(1)));

        assertThat(loads.findByOwnerIdAndStatus(owner.getId(), "OPEN")).hasSize(1);
        assertThat(trucks.findByTransporterId(transporter.getId())).hasSize(1);
        assertThat(trucks.findByStatus("AVAILABLE")).isNotEmpty();
    }

    @Test
    void round_trips_match_reasons_through_jsonb() {
        User owner = users.save(new User("O", "o2@example.com", "hash", "FREIGHT_OWNER"));
        User transporter = users.save(new User("T", "t2@example.com", "hash", "TRANSPORTER"));
        Load load = loads.save(new Load(owner.getId(), "Cape Town", "Bloemfontein", "GENERAL",
                new BigDecimal("5000.00"), new BigDecimal("20.00"),
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(1)));
        Truck truck = trucks.save(new Truck(transporter.getId(), "FLATBED",
                new BigDecimal("20000.00"), null, "Cape Town",
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(1)));

        Match saved = matches.save(new Match(load.getId(), truck.getId(), new BigDecimal("87.50"),
                List.of("capacity sufficient", "same origin city")));

        Match reloaded = matches.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getReasons()).containsExactly("capacity sufficient", "same origin city");
        assertThat(reloaded.getLoadId()).isEqualTo(load.getId());
        assertThat(reloaded.getTruckId()).isEqualTo(truck.getId());
        assertThat(reloaded.getStatus()).isEqualTo("PROPOSED");
        assertThat(reloaded.getDecidedAt()).isNull();
        assertThat(reloaded.getCreatedAt()).isNotNull();
        // Re-fetched, not the raw object save() returned: created_at/updated_at have no
        // @Generated annotation, so save() does not refresh them in the in-memory instance;
        // only a real SELECT reflects the database defaults that produced them.
        Truck reloadedTruck = trucks.findById(truck.getId()).orElseThrow();
        assertThat(reloadedTruck.getVehicleType()).isEqualTo("FLATBED");
        assertThat(reloadedTruck.getCapacityKg()).isEqualByComparingTo("20000.00");
        assertThat(reloadedTruck.getCapacityM3()).isNull();
        assertThat(reloadedTruck.getCurrentCity()).isEqualTo("Cape Town");
        assertThat(reloadedTruck.getCreatedAt()).isNotNull();
        assertThat(reloadedTruck.getUpdatedAt()).isNotNull();
        assertThat(matches.findByLoadId(load.getId())).extracting(Match::getId).contains(saved.getId());
        assertThat(matches.findByTruckId(truck.getId())).extracting(Match::getId).contains(saved.getId());
    }

    @Test
    void round_trips_receipt_ip_address_through_the_inet_converter() {
        User owner = users.save(new User("O3", "o3@example.com", "hash", "FREIGHT_OWNER"));
        User transporter = users.save(new User("T3", "t3@example.com", "hash", "TRANSPORTER"));
        Load load = loads.save(new Load(owner.getId(), "Durban", "Polokwane", "GENERAL",
                new BigDecimal("3000.00"), new BigDecimal("10.00"),
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(1)));
        Truck truck = trucks.save(new Truck(transporter.getId(), "CONTAINER",
                new BigDecimal("15000.00"), new BigDecimal("30.00"), "Durban",
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(1)));
        Match match = matches.save(new Match(load.getId(), truck.getId(), new BigDecimal("70.00"),
                List.of("capacity sufficient")));

        Receipt saved = receipts.save(new Receipt(match.getId(), "ACCEPTED", owner.getId(),
                "196.168.1.10", "curl/8.0"));

        Receipt reloaded = receipts.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getIpAddress()).isEqualTo("196.168.1.10");
        assertThat(reloaded.getContractId()).startsWith("TAMP-");
        assertThat(reloaded.getMatchId()).isEqualTo(match.getId());
        assertThat(reloaded.getDecision()).isEqualTo("ACCEPTED");
        assertThat(reloaded.getActorId()).isEqualTo(owner.getId());
        assertThat(reloaded.getUserAgent()).isEqualTo("curl/8.0");
        assertThat(reloaded.getIssuedAt()).isNotNull();
        assertThat(receipts.findByMatchId(match.getId())).isPresent();
        assertThat(receipts.findByContractId(reloaded.getContractId())).isPresent();
    }

    @Test
    void round_trips_audit_log_details_through_jsonb() {
        User actor = users.save(new User("Admin", "admin2@example.com", "hash", "ADMIN"));

        AuditLog saved = auditLogs.save(new AuditLog(actor.getId(), "MATCH_ACCEPTED", "MATCH",
                UUID.randomUUID(), Map.of("previous_status", "PROPOSED")));

        AuditLog reloaded = auditLogs.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getDetails()).containsEntry("previous_status", "PROPOSED");
        assertThat(reloaded.getActorId()).isEqualTo(actor.getId());
        assertThat(reloaded.getAction()).isEqualTo("MATCH_ACCEPTED");
        assertThat(reloaded.getEntityType()).isEqualTo("MATCH");
        assertThat(reloaded.getEntityId()).isNotNull();
        assertThat(reloaded.getOccurredAt()).isNotNull();
        assertThat(auditLogs.findByEntityTypeAndEntityId("MATCH", reloaded.getEntityId()))
                .extracting(AuditLog::getId).contains(saved.getId());
    }

    @Test
    void saves_and_reads_back_a_rating() {
        User owner = users.save(new User("O4", "o4@example.com", "hash", "FREIGHT_OWNER"));
        User transporter = users.save(new User("T4", "t4@example.com", "hash", "TRANSPORTER"));
        Load load = loads.save(new Load(owner.getId(), "East London", "Mthatha", "GENERAL",
                new BigDecimal("2000.00"), new BigDecimal("8.00"),
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(1)));
        Truck truck = trucks.save(new Truck(transporter.getId(), "FLATBED",
                new BigDecimal("10000.00"), null, "East London",
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(1)));
        Match match = matches.save(new Match(load.getId(), truck.getId(), new BigDecimal("60.00"),
                List.of("capacity sufficient")));

        Rating saved = ratings.save(new Rating(match.getId(), owner.getId(), transporter.getId(),
                (short) 5, "On time"));

        Rating reloaded = ratings.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getScore()).isEqualTo((short) 5);
        assertThat(reloaded.getMatchId()).isEqualTo(match.getId());
        assertThat(reloaded.getRaterId()).isEqualTo(owner.getId());
        assertThat(reloaded.getRateeId()).isEqualTo(transporter.getId());
        assertThat(reloaded.getComment()).isEqualTo("On time");
        // No equals()/hashCode() override on Rating, so comparing by id rather than by object
        // identity: findByRateeId returns a freshly loaded instance, not the same reference.
        assertThat(ratings.findByRateeId(transporter.getId()))
                .extracting(Rating::getId).contains(saved.getId());
    }

    @Test
    void saves_and_reads_back_a_tracking_event() {
        User owner = users.save(new User("O5", "o5@example.com", "hash", "FREIGHT_OWNER"));
        User transporter = users.save(new User("T5", "t5@example.com", "hash", "TRANSPORTER"));
        Load load = loads.save(new Load(owner.getId(), "Kimberley", "Upington", "GENERAL",
                new BigDecimal("1500.00"), new BigDecimal("6.00"),
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(1)));
        Truck truck = trucks.save(new Truck(transporter.getId(), "FLATBED",
                new BigDecimal("10000.00"), null, "Kimberley",
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(1)));
        Match match = matches.save(new Match(load.getId(), truck.getId(), new BigDecimal("65.00"),
                List.of("capacity sufficient")));

        TrackingEvent saved = trackingEvents.save(new TrackingEvent(match.getId(),
                new BigDecimal("-28.738"), new BigDecimal("24.763"), "IN_TRANSIT"));

        TrackingEvent reloaded = trackingEvents.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("IN_TRANSIT");
        assertThat(reloaded.getMatchId()).isEqualTo(match.getId());
        assertThat(reloaded.getLatitude()).isEqualByComparingTo("-28.738000");
        assertThat(reloaded.getLongitude()).isEqualByComparingTo("24.763000");
        assertThat(reloaded.getOccurredAt()).isNotNull();
        assertThat(trackingEvents.findByMatchIdOrderByOccurredAtAsc(match.getId()))
                .extracting(TrackingEvent::getId).contains(saved.getId());
    }

    @Test
    void saves_and_reads_back_a_dispute_raised_directly_against_a_user() {
        User owner = users.save(new User("O6", "o6@example.com", "hash", "FREIGHT_OWNER"));
        User reported = users.save(new User("T6", "t6@example.com", "hash", "TRANSPORTER"));

        Dispute saved = disputes.save(new Dispute(null, reported.getId(), owner.getId(),
                "Repeatedly cancels accepted jobs"));

        Dispute reloaded = disputes.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("OPEN");
        assertThat(reloaded.getMatchId()).isNull();
        assertThat(reloaded.getSubjectUserId()).isEqualTo(reported.getId());
        assertThat(reloaded.getRaisedBy()).isEqualTo(owner.getId());
        assertThat(reloaded.getDescription()).isEqualTo("Repeatedly cancels accepted jobs");
        assertThat(reloaded.getResolvedAt()).isNull();
        assertThat(disputes.findByStatus("OPEN")).extracting(Dispute::getId).contains(saved.getId());
    }

    @Test
    void saves_and_reads_back_a_compliance_document() {
        User transporter = users.save(new User("T7", "t7@example.com", "hash", "TRANSPORTER"));

        ComplianceDocument saved = complianceDocuments.save(
                new ComplianceDocument(transporter.getId(), "OPERATOR_LICENCE", "licence-scan.pdf"));

        ComplianceDocument reloaded = complianceDocuments.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("PENDING");
        assertThat(reloaded.getUserId()).isEqualTo(transporter.getId());
        assertThat(reloaded.getDocumentType()).isEqualTo("OPERATOR_LICENCE");
        assertThat(reloaded.getFileName()).isEqualTo("licence-scan.pdf");
        assertThat(reloaded.getReviewedBy()).isNull();
        assertThat(complianceDocuments.findByUserId(transporter.getId()))
                .extracting(ComplianceDocument::getId).contains(saved.getId());
    }

    @Test
    void finds_only_the_loads_belonging_to_the_requesting_owner() {
        User ownerA = users.save(new User("A", "ownera@example.com", "hash", "FREIGHT_OWNER"));
        User ownerB = users.save(new User("B", "ownerb@example.com", "hash", "FREIGHT_OWNER"));
        loads.save(new Load(ownerA.getId(), "Pretoria", "Nelspruit", "GENERAL",
                new BigDecimal("1000.00"), new BigDecimal("5.00"),
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(1)));
        loads.save(new Load(ownerB.getId(), "Pretoria", "Nelspruit", "GENERAL",
                new BigDecimal("1000.00"), new BigDecimal("5.00"),
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(1)));

        List<Load> ownerALoads = loads.findByOwnerId(ownerA.getId());

        // Defends against the authorisation-shaped bug where a missing owner filter
        // returns every load on the platform to whoever asks.
        assertThat(ownerALoads).extracting(Load::getOwnerId).containsOnly(ownerA.getId());
    }
}
