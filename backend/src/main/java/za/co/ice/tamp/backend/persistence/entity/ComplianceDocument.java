package za.co.ice.tamp.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Maps {@code compliance_documents} (db/migrations/V11__compliance_documents.sql). */
@Entity
@Table(name = "compliance_documents")
public class ComplianceDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(insertable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "file_name")
    private String fileName;

    @Column(insertable = false)
    private String status;

    @Column(name = "uploaded_at", insertable = false, updatable = false)
    private OffsetDateTime uploadedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    protected ComplianceDocument() {
    }

    public ComplianceDocument(UUID userId, String documentType, String fileName) {
        this.userId = userId;
        this.documentType = documentType;
        this.fileName = fileName;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStatus() {
        return status;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    /** Moves the document to a reviewed state, setting all three columns the schema requires together. */
    public void review(String status, UUID reviewedBy) {
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = OffsetDateTime.now();
    }
}
