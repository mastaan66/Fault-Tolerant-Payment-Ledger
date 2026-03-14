package com.stripe.ledger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class LedgerService {
    private final AccountRepository accountRepository;

    @Transactional
    public void transfer(TransferRequest request) {
        // Prevent deadlocks by imposing consistent lock ordering
        String first = request.getFromAccount().compareTo(request.getToAccount()) < 0 
                ? request.getFromAccount() : request.getToAccount();
        String second = request.getFromAccount().compareTo(request.getToAccount()) < 0 
                ? request.getToAccount() : request.getFromAccount();

        Account acc1 = accountRepository.findByAccountNumber(first)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + first));
        Account acc2 = accountRepository.findByAccountNumber(second)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + second));

        Account from = acc1.getAccountNumber().equals(request.getFromAccount()) ? acc1 : acc2;
        Account to = acc1.getAccountNumber().equals(request.getToAccount()) ? acc1 : acc2;

        if (from.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }

        from.setBalance(from.getBalance().subtract(request.getAmount()));
        to.setBalance(to.getBalance().add(request.getAmount()));

        accountRepository.save(from);
        accountRepository.save(to);
    }
}
