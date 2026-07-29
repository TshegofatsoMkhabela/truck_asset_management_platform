package za.co.ice.tamp.backend.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ice.tamp.backend.persistence.entity.ComplianceDocument;

public interface ComplianceDocumentRepository extends JpaRepository<ComplianceDocument, UUID> {

    /** Backs FR-02/FR-10: an admin reviewing one user's paperwork. */
    List<ComplianceDocument> findByUserId(UUID userId);
}
