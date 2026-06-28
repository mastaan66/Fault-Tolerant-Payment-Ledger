package io.github.mastaan66.ledger;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        String id,
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        TransferStatus status,
        Instant createdAt) {

    static TransferResponse from(TransferRecord transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromAccount(),
                transfer.getToAccount(),
                transfer.getAmount(),
                transfer.getStatus(),
                transfer.getCreatedAt());
    }
}
