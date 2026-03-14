package com.stripe.ledger;

import lombok.Data;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
public class TransferRequest {
    @NotNull
    private String fromAccount;

    @NotNull
    private String toAccount;

    @NotNull
    @Positive
    private BigDecimal amount;
}
