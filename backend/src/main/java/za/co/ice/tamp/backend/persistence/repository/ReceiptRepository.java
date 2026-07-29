package za.co.ice.tamp.backend.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ice.tamp.backend.persistence.entity.Receipt;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    Optional<Receipt> findByMatchId(UUID matchId);

    Optional<Receipt> findByContractId(String contractId);
}
