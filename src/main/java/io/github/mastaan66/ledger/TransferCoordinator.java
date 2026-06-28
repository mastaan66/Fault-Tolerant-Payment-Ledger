package io.github.mastaan66.ledger;

import org.springframework.stereotype.Service;

@Service
public class TransferCoordinator {

    private final LedgerService ledgerService;

    public TransferCoordinator(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    public TransferResult transfer(String idempotencyKey, TransferRequest request) {
        try {
            return ledgerService.executeTransfer(idempotencyKey, request);
        } catch (ConcurrentIdempotencyClaimException concurrentClaim) {
            // The unique idempotency-key constraint chooses one winner. Once that
            // transaction commits, the losing request safely replays its result.
            return ledgerService.replayAfterConcurrentClaim(idempotencyKey, request);
        }
    }
}
