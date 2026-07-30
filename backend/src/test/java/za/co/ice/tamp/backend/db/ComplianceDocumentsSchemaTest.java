package za.co.ice.tamp.backend.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Proves the constraints on {@code compliance_documents}.
 *
 * <p>Only metadata is stored. Brief section 2.2 asks for "mock compliance-document metadata",
 * and section 4.1 keeps real document handling out of scope, so no file content is persisted.
 */
class ComplianceDocumentsSchemaTest extends MigratedSchemaTestBase {

    private String transporter;
    private String admin;

    @BeforeEach
    void seedUsers() throws SQLException {
        transporter = insertUser("transporter@example.com", "TRANSPORTER");
        admin = insertUser("admin@example.com", "ADMIN");
    }

    @Test
    void rejects_a_document_for_a_nonexistent_user() {
        assertThatThrownBy(() -> insertDocument(GHOST, "OPERATOR_LICENCE"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("compliance_documents_user_id_fkey");
    }

    @Test
    void rejects_an_unknown_document_type() {
        assertThatThrownBy(() -> insertDocument(transporter, "PROOF_OF_VIBES"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("compliance_documents_document_type_check");
    }

    @Test
    void defaults_a_newly_uploaded_document_to_pending() throws SQLException {
        insertDocument(transporter, "OPERATOR_LICENCE");

        assertThat(single("SELECT status FROM compliance_documents")).isEqualTo("PENDING");
    }

    @Test
    void rejects_a_reviewed_document_with_no_reviewer_recorded() throws SQLException {
        insertDocument(transporter, "OPERATOR_LICENCE");

        assertThatThrownBy(() -> execute("UPDATE compliance_documents SET status = 'APPROVED'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("compliance_documents_review_consistency_check");
    }

    @Test
    void records_who_approved_a_document_and_when() throws SQLException {
        insertDocument(transporter, "OPERATOR_LICENCE");

        execute("UPDATE compliance_documents SET status = 'APPROVED', reviewed_by = '" + admin
                + "', reviewed_at = now()");

        assertThat(single("SELECT reviewed_by::text FROM compliance_documents")).isEqualTo(admin);
    }

    @Test
    void allows_a_user_to_hold_several_documents_of_different_types() throws SQLException {
        insertDocument(transporter, "OPERATOR_LICENCE");
        insertDocument(transporter, "INSURANCE");

        assertThat(single("SELECT count(*)::text FROM compliance_documents")).isEqualTo("2");
    }

    private void insertDocument(String userId, String documentType) throws SQLException {
        try (PreparedStatement insert = db.prepareStatement(
                "INSERT INTO compliance_documents (user_id, document_type, file_name) "
                        + "VALUES (?::uuid, ?, 'licence-scan.pdf')")) {
            insert.setString(1, userId);
            insert.setString(2, documentType);
            insert.execute();
        }
    }
}
