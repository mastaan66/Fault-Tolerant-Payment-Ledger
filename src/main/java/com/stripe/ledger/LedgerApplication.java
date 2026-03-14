package com.stripe.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import java.math.BigDecimal;

@SpringBootApplication
public class LedgerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LedgerApplication.class, args);
	}

	@Bean
	CommandLineRunner initDatabase(AccountRepository repository) {
		return args -> {
			Account acc1 = new Account();
			acc1.setAccountNumber("AC100");
			acc1.setBalance(new BigDecimal("5000.00"));
			repository.save(acc1);

			Account acc2 = new Account();
			acc2.setAccountNumber("AC200");
			acc2.setBalance(new BigDecimal("1000.00"));
			repository.save(acc2);
		};
	}
}
