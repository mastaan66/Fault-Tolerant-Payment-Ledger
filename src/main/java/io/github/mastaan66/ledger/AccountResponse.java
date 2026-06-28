package io.github.mastaan66.ledger;

import java.math.BigDecimal;

public record AccountResponse(String accountNumber, BigDecimal balance, Long version) {

    static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountNumber(),
                account.getBalance(),
                account.getVersion());
    }
}
