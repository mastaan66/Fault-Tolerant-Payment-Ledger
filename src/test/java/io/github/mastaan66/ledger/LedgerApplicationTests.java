package io.github.mastaan66.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "ledger.seed-demo-data=false")
@AutoConfigureMockMvc
class LedgerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private TransferCoordinator transferCoordinator;

    @BeforeEach
    void resetDatabase() {
        idempotencyKeyRepository.deleteAll();
        transferRepository.deleteAll();
        accountRepository.deleteAll();
        accountRepository.saveAll(List.of(
                new Account("AC100", new BigDecimal("5000.00")),
                new Account("AC200", new BigDecimal("1000.00"))));
    }

    @Test
    void transfersMoneyAndExposesAuditRecord() throws Exception {
        MvcResult transfer = postTransfer("pay-001", "AC100", "AC200", "500.00")
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "false"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(
                        "/api/ledger/transfers/")))
                .andExpect(jsonPath("$.fromAccount").value("AC100"))
                .andExpect(jsonPath("$.toAccount").value("AC200"))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn();

        String transferId = com.jayway.jsonpath.JsonPath.read(
                transfer.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/ledger/transfers/{id}", transferId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transferId));

        assertBalance("AC100", "4500.00");
        assertBalance("AC200", "1500.00");
        assertThat(transferRepository.count()).isEqualTo(1);
    }

    @Test
    void replaysTheOriginalResultWithoutMovingMoneyTwice() throws Exception {
        String firstBody = postTransfer("pay-retry", "AC100", "AC200", "125.00")
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "false"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String replayBody = postTransfer("pay-retry", "AC100", "AC200", "125.0")
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(replayBody).isEqualTo(firstBody);
        assertBalance("AC100", "4875.00");
        assertBalance("AC200", "1125.00");
        assertThat(transferRepository.count()).isEqualTo(1);
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsReusingAKeyForDifferentRequestData() throws Exception {
        postTransfer("pay-conflict", "AC100", "AC200", "100.00")
                .andExpect(status().isCreated());

        postTransfer("pay-conflict", "AC100", "AC200", "101.00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Idempotency conflict"));

        assertBalance("AC100", "4900.00");
        assertBalance("AC200", "1100.00");
        assertThat(transferRepository.count()).isEqualTo(1);
    }

    @Test
    void rollsBackFailedTransferAndAllowsSafeRetry() throws Exception {
        postTransfer("pay-recover", "AC100", "AC200", "6000.00")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(
                        "Insufficient funds in account: AC100"));

        assertThat(idempotencyKeyRepository.count()).isZero();
        assertThat(transferRepository.count()).isZero();
        assertBalance("AC100", "5000.00");
        assertBalance("AC200", "1000.00");

        postTransfer("pay-recover", "AC100", "AC200", "100.00")
                .andExpect(status().isCreated());

        assertBalance("AC100", "4900.00");
        assertBalance("AC200", "1100.00");
    }

    @Test
    void returnsUsefulErrorsForInvalidTransfers() throws Exception {
        postTransfer("same-account", "AC100", "AC100", "10.00")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(
                        "Source and destination accounts must be different"));

        postTransfer("missing-account", "AC100", "UNKNOWN", "10.00")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account not found: UNKNOWN"));

        postTransfer("invalid-amount", "AC100", "AC200", "10.001")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());

        mockMvc.perform(post("/api/ledger/transfers")
                        .header("Idempotency-Key", "blank-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromAccount": " ",
                                  "toAccount": "AC200",
                                  "amount": 10.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fromAccount").exists());
    }

    @Test
    void serializesConcurrentRequestsSharingAnIdempotencyKey() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            CountDownLatch start = new CountDownLatch(1);
            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                TransferRequest request =
                        new TransferRequest("AC100", "AC200", new BigDecimal("75.00"));

                CompletableFuture<TransferResult> first = CompletableFuture.supplyAsync(
                        () -> transferAfter(start, request), executor);
                CompletableFuture<TransferResult> second = CompletableFuture.supplyAsync(
                        () -> transferAfter(start, request), executor);
                start.countDown();

                List<TransferResult> results = List.of(first.join(), second.join());
                assertThat(results).extracting(TransferResult::replayed)
                        .containsExactlyInAnyOrder(false, true);
                assertThat(results).extracting(result -> result.transfer().id())
                        .containsOnly(results.getFirst().transfer().id());
            }

            assertBalance("AC100", "4925.00");
            assertBalance("AC200", "1075.00");
            assertThat(transferRepository.count()).isEqualTo(1);
        });
    }

    private TransferResult transferAfter(
            CountDownLatch start,
            TransferRequest request) {
        try {
            start.await();
            return transferCoordinator.transfer("concurrent-key", request);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private org.springframework.test.web.servlet.ResultActions postTransfer(
            String key,
            String from,
            String to,
            String amount) throws Exception {
        return mockMvc.perform(post("/api/ledger/transfers")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "fromAccount": "%s",
                          "toAccount": "%s",
                          "amount": %s
                        }
                        """.formatted(from, to, amount)));
    }

    private void assertBalance(String accountNumber, String expected) {
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow();
        assertThat(account.getBalance()).isEqualByComparingTo(expected);
    }
}
