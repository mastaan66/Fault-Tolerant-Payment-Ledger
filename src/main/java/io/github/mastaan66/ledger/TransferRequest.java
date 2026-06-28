package io.github.mastaan66.ledger;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TransferRequest(
        @NotBlank
        @Size(max = 40)
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*")
        String fromAccount,

        @NotBlank
        @Size(max = 40)
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*")
        String toAccount,

        @NotNull
        @Positive
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount) {
}
