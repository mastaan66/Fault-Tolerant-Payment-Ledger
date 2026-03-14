package com.stripe.ledger;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
public class IdempotencyKey {
    @Id
    private String requestKey;

    private String responsePayload;
    private int statusCode;
}
