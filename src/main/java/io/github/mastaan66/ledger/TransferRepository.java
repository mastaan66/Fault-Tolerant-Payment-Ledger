package io.github.mastaan66.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<TransferRecord, String> {
}
