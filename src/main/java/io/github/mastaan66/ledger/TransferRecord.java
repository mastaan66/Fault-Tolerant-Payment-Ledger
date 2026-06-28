package io.github.mastaan66.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transfers")
public class TransferRecord {

    @Id
    @Column(length = 36, updatable = false)
    private String id;

    @Column(name = "from_account", nullable = false, length = 40, updatable = false)
    private String fromAccount;

    @Column(name = "to_account", nullable = false, length = 40, updatable = false)
    private String toAccount;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TransferStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TransferRecord() {
    }

    public TransferRecord(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            Instant createdAt) {
        this.id = UUID.randomUUID().toString();
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.status = TransferStatus.COMPLETED;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
