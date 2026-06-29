package io.github.mastaan66.ledger;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public LedgerService(
            AccountRepository accountRepository,
            TransferRepository transferRepository,
            IdempotencyKeyRepository idempotencyKeyRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Transactional
    public TransferResult executeTransfer(String idempotencyKey, TransferRequest request) {
        validateTransfer(request);
        String fingerprint = fingerprint(request);

        var existing = idempotencyKeyRepository.findById(idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), fingerprint);
        }

        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        var claim = new IdempotencyKey(idempotencyKey, fingerprint, now);
        try {
            idempotencyKeyRepository.saveAndFlush(claim);
        } catch (DataIntegrityViolationException duplicateKey) {
            throw new ConcurrentIdempotencyClaimException(duplicateKey);
        }

        String firstAccount = request.fromAccount().compareTo(request.toAccount()) < 0
                ? request.fromAccount()
                : request.toAccount();
        String secondAccount = firstAccount.equals(request.fromAccount())
                ? request.toAccount()
                : request.fromAccount();

        Account first = findAccountForUpdate(firstAccount);
        Account second = findAccountForUpdate(secondAccount);
        Account from = first.getAccountNumber().equals(request.fromAccount()) ? first : second;
        Account to = first.getAccountNumber().equals(request.toAccount()) ? first : second;

        BigDecimal amount = request.amount().setScale(2);
        from.debit(amount);
        to.credit(amount);

        TransferRecord transfer = transferRepository.save(
                new TransferRecord(
                        request.fromAccount(),
                        request.toAccount(),
                        amount,
                        now));
        claim.completeWith(transfer.getId());
        idempotencyKeyRepository.saveAndFlush(claim);

        return new TransferResult(TransferResponse.from(transfer), false);
    }

    @Transactional(readOnly = true)
    public TransferResult replayAfterConcurrentClaim(
            String idempotencyKey,
            TransferRequest request) {
        IdempotencyKey existing = idempotencyKeyRepository.findById(idempotencyKey)
                .orElseThrow(() -> new IncompleteIdempotencyRecordException(idempotencyKey));
        return replay(existing, fingerprint(request));
    }

    @Transactional(readOnly = true)
    public TransferResponse findTransfer(String transferId) {
        return transferRepository.findById(transferId)
                .map(TransferResponse::from)
                .orElseThrow(() -> new TransferNotFoundException(transferId));
    }

    @Transactional(readOnly = true)
    public AccountResponse findAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(AccountResponse::from)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    @Transactional(readOnly = true)
    public AccountActivityResponse findAccountActivity(
            String accountNumber,
            TransferDirection direction,
            int page,
            int size) {
        AccountResponse account = findAccount(accountNumber);
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<TransferRecord> transfers = switch (direction) {
            case ALL -> transferRepository.findByFromAccountOrToAccount(
                    accountNumber, accountNumber, pageRequest);
            case INCOMING -> transferRepository.findByToAccount(accountNumber, pageRequest);
            case OUTGOING -> transferRepository.findByFromAccount(accountNumber, pageRequest);
        };

        TransferRepository.TransferTotals incoming =
                transferRepository.summarizeIncoming(accountNumber);
        TransferRepository.TransferTotals outgoing =
                transferRepository.summarizeOutgoing(accountNumber);

        var summary = new AccountActivityResponse.ActivitySummary(
                incoming.getTransferCount(),
                amountOrZero(incoming.getTotalAmount()),
                outgoing.getTransferCount(),
                amountOrZero(outgoing.getTotalAmount()));
        var metadata = new AccountActivityResponse.PageMetadata(
                transfers.getNumber(),
                transfers.getSize(),
                transfers.getTotalElements(),
                transfers.getTotalPages(),
                transfers.isFirst(),
                transfers.isLast());
        var entries = transfers.getContent().stream()
                .map(transfer -> AccountActivityEntry.from(transfer, accountNumber))
                .toList();

        return new AccountActivityResponse(account, summary, direction, entries, metadata);
    }

    private TransferResult replay(IdempotencyKey existing, String fingerprint) {
        if (!MessageDigest.isEqual(
                existing.getRequestFingerprint().getBytes(StandardCharsets.US_ASCII),
                fingerprint.getBytes(StandardCharsets.US_ASCII))) {
            throw new IdempotencyConflictException();
        }
        if (existing.getTransferId() == null) {
            throw new IncompleteIdempotencyRecordException(existing.getRequestKey());
        }
        TransferRecord transfer = transferRepository.findById(existing.getTransferId())
                .orElseThrow(() ->
                        new IncompleteIdempotencyRecordException(existing.getRequestKey()));
        return new TransferResult(TransferResponse.from(transfer), true);
    }

    private Account findAccountForUpdate(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount == null ? new BigDecimal("0.00") : amount.setScale(2);
    }

    private void validateTransfer(TransferRequest request) {
        if (request.fromAccount().equals(request.toAccount())) {
            throw new InvalidTransferException(
                    "Source and destination accounts must be different");
        }
    }

    private String fingerprint(TransferRequest request) {
        String canonicalRequest = request.fromAccount()
                + "\n" + request.toAccount()
                + "\n" + request.amount().stripTrailingZeros().toPlainString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
