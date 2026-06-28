package io.github.mastaan66.ledger;

final class AccountNotFoundException extends RuntimeException {
    AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
    }
}

final class TransferNotFoundException extends RuntimeException {
    TransferNotFoundException(String transferId) {
        super("Transfer not found: " + transferId);
    }
}

final class InsufficientFundsException extends RuntimeException {
    InsufficientFundsException(String accountNumber) {
        super("Insufficient funds in account: " + accountNumber);
    }
}

final class InvalidTransferException extends RuntimeException {
    InvalidTransferException(String message) {
        super(message);
    }
}

final class IdempotencyConflictException extends RuntimeException {
    IdempotencyConflictException() {
        super("The Idempotency-Key was already used with a different request");
    }
}

final class ConcurrentIdempotencyClaimException extends RuntimeException {
    ConcurrentIdempotencyClaimException(Throwable cause) {
        super("Another request claimed the same idempotency key", cause);
    }
}

final class IncompleteIdempotencyRecordException extends RuntimeException {
    IncompleteIdempotencyRecordException(String requestKey) {
        super("No completed transfer was found for Idempotency-Key: " + requestKey);
    }
}
