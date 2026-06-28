package io.github.mastaan66.ledger;

import java.math.BigDecimal;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerApplication.class, args);
    }

    @Bean
    @ConditionalOnProperty(
            name = "ledger.seed-demo-data",
            havingValue = "true",
            matchIfMissing = true)
    ApplicationRunner seedDemoAccounts(AccountRepository accountRepository) {
        return args -> {
            createAccountIfMissing(accountRepository, "AC100", new BigDecimal("5000.00"));
            createAccountIfMissing(accountRepository, "AC200", new BigDecimal("1000.00"));
        };
    }

    private void createAccountIfMissing(
            AccountRepository accountRepository,
            String accountNumber,
            BigDecimal openingBalance) {
        if (!accountRepository.existsByAccountNumber(accountNumber)) {
            accountRepository.save(new Account(accountNumber, openingBalance));
        }
    }
}
