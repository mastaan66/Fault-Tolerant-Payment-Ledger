package io.github.mastaan66.ledger;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountActivityEntry(
        String transferId,
        TransferDirection direction,
        String counterpartyAccount,
        BigDecimal amount,
        TransferStatus status,
        Instant createdAt) {

    static AccountActivityEntry from(TransferRecord transfer, String accountNumber) {
        boolean outgoing = transfer.getFromAccount().equals(accountNumber);
        return new AccountActivityEntry(
                transfer.getId(),
                outgoing ? TransferDirection.OUTGOING : TransferDirection.INCOMING,
                outgoing ? transfer.getToAccount() : transfer.getFromAccount(),
                transfer.getAmount(),
                transfer.getStatus(),
                transfer.getCreatedAt());
    }
}
