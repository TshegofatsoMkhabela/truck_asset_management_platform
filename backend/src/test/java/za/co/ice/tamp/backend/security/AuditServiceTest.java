package za.co.ice.tamp.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.ice.tamp.backend.persistence.entity.AuditLog;
import za.co.ice.tamp.backend.persistence.repository.AuditLogRepository;

/**
 * Proves {@link AuditService} writes an {@link AuditLog} with the fields it was given, defending
 * against a typo'd field mapping silently producing an unusable audit trail, since #9's entire
 * point is that later issues call this one place rather than re-implementing the write.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository repository;

    @Test
    void writesAuditRowWithExpectedFields() {
        AuditService service = new AuditService(repository);
        UUID actorId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.record(actorId, "USER_REGISTERED", "User", entityId, Map.of("email", "a@b.com"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getActorId()).isEqualTo(actorId);
        assertThat(saved.getAction()).isEqualTo("USER_REGISTERED");
        assertThat(saved.getEntityType()).isEqualTo("User");
        assertThat(saved.getEntityId()).isEqualTo(entityId);
        assertThat(saved.getDetails()).containsEntry("email", "a@b.com");
    }
}
