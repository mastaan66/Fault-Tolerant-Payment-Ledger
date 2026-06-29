package io.github.mastaan66.ledger;

import java.math.BigDecimal;
import java.util.List;

public record AccountActivityResponse(
        AccountResponse account,
        ActivitySummary summary,
        TransferDirection direction,
        List<AccountActivityEntry> entries,
        PageMetadata page) {

    public record ActivitySummary(
            long incomingCount,
            BigDecimal incomingAmount,
            long outgoingCount,
            BigDecimal outgoingAmount) {
    }

    public record PageMetadata(
            int number,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last) {
    }
}
