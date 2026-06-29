package io.github.mastaan66.ledger;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final TransferCoordinator transferCoordinator;
    private final LedgerService ledgerService;

    public LedgerController(
            TransferCoordinator transferCoordinator,
            LedgerService ledgerService) {
        this.transferCoordinator = transferCoordinator;
        this.ledgerService = ledgerService;
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(
            @RequestHeader("Idempotency-Key")
            @NotBlank
            @Size(max = 100)
            String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        TransferResult result = transferCoordinator.transfer(idempotencyKey, request);
        URI location = URI.create("/api/ledger/transfers/" + result.transfer().id());

        return ResponseEntity.status(HttpStatus.CREATED)
                .location(location)
                .header("Idempotent-Replayed", Boolean.toString(result.replayed()))
                .body(result.transfer());
    }

    @GetMapping("/transfers/{transferId}")
    public TransferResponse findTransfer(@PathVariable String transferId) {
        return ledgerService.findTransfer(transferId);
    }

    @GetMapping("/accounts/{accountNumber}")
    public AccountResponse findAccount(@PathVariable String accountNumber) {
        return ledgerService.findAccount(accountNumber);
    }

    @GetMapping("/accounts/{accountNumber}/activity")
    public AccountActivityResponse findAccountActivity(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "ALL") TransferDirection direction,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ledgerService.findAccountActivity(accountNumber, direction, page, size);
    }
}
