package com.stripe.ledger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerController {
    private final LedgerService ledgerService;
    private final IdempotencyKeyRepository idempotencyRepo;

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        // Check for idempotency
        IdempotencyKey existingKey = idempotencyRepo.findById(idempotencyKey).orElse(null);
        if (existingKey != null) {
            return ResponseEntity.status(existingKey.getStatusCode())
                    .body(existingKey.getResponsePayload());
        }

        try {
            ledgerService.transfer(request);
            
            // Save idempotency record
            IdempotencyKey newKey = new IdempotencyKey();
            newKey.setRequestKey(idempotencyKey);
            newKey.setStatusCode(200);
            newKey.setResponsePayload("Transfer successful");
            idempotencyRepo.save(newKey);

            return ResponseEntity.ok("Transfer successful");
        } catch (Exception e) {
            IdempotencyKey failedKey = new IdempotencyKey();
            failedKey.setRequestKey(idempotencyKey);
            failedKey.setStatusCode(400); // Or 500 depending on error, keeping simple
            failedKey.setResponsePayload("Transfer failed: " + e.getMessage());
            idempotencyRepo.save(failedKey);

            return ResponseEntity.badRequest().body("Transfer failed: " + e.getMessage());
        }
    }
}
