package io.github.mastaan66.ledger;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    @Id
    @Column(name = "request_key", length = 100, nullable = false, updatable = false)
    private String requestKey;

    @Column(name = "request_fingerprint", length = 64, nullable = false, updatable = false)
    private String requestFingerprint;

    @Column(name = "transfer_id", length = 36)
    private String transferId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyKey() {
    }

    public IdempotencyKey(String requestKey, String requestFingerprint, Instant createdAt) {
        this.requestKey = requestKey;
        this.requestFingerprint = requestFingerprint;
        this.createdAt = createdAt;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public String getTransferId() {
        return transferId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void completeWith(String transferId) {
        this.transferId = transferId;
    }
}
