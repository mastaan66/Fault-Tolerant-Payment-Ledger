# Fault-Tolerant Payment Ledger

[![CI](https://github.com/mastaan66/Fault-Tolerant-Payment-Ledger/actions/workflows/ci.yml/badge.svg)](https://github.com/mastaan66/Fault-Tolerant-Payment-Ledger/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00.svg)](https://openjdk.org/projects/jdk/21/)

An educational Spring Boot reference service for the hard parts of moving money:
atomic balance updates, safe network retries, concurrent requests, deterministic
locking, and an auditable transfer record.

This repository is intentionally small enough to study. It demonstrates useful
reliability patterns without claiming to be a bank-ready general ledger.

## Why this project is useful

- A transfer, both balance changes, its audit record, and its idempotency result
  commit in one database transaction.
- Concurrent requests sharing one idempotency key are resolved by a database
  uniqueness constraint; only one request can move money.
- Reusing a key with different request data returns `409 Conflict` instead of
  silently replaying the wrong operation.
- Accounts are locked in stable lexical order to reduce deadlock risk.
- Failed transfers roll back completely and can be retried with the same key.
- API errors use RFC 9457-style `application/problem+json` responses.
- Integration tests exercise real HTTP, JPA, rollback, and concurrency behavior.

## What it does not promise

This is a reference implementation, not production financial infrastructure.
It has no authentication, authorization, currency model, immutable double-entry
journal, external payment-rail integration, or regulatory controls. The Docker
profile currently uses Hibernate schema updates for convenience. See
[the roadmap](docs/ROADMAP.md) for the work required before serious deployment.

## Architecture

```text
HTTP request
    |
    v
LedgerController -> TransferCoordinator -> LedgerService
                                            |
                         +------------------+------------------+
                         |                  |                  |
                  idempotency key    locked accounts    transfer record
                         +------------------+------------------+
                                  one ACID transaction
```

The mutable account balance is a fast balance projection. The `transfers`
table is an audit trail of completed transfers. A future production-oriented
version should add immutable debit/credit journal entries and reconcile the
projection from that journal.

More detail: [Architecture](docs/ARCHITECTURE.md) · [API contract](docs/API.md)

## Quick start

Requirements: Java 21. The Maven Wrapper downloads the correct Maven version.

```bash
git clone https://github.com/mastaan66/Fault-Tolerant-Payment-Ledger.git
cd Fault-Tolerant-Payment-Ledger
./mvnw spring-boot:run
```

The service starts at `http://localhost:8081` with an in-memory H2 database and
two demo accounts:

| Account | Opening balance |
| --- | ---: |
| `AC100` | `5000.00` |
| `AC200` | `1000.00` |

Create a transfer:

```bash
curl --include --request POST http://localhost:8081/api/ledger/transfers \
  --header 'Content-Type: application/json' \
  --header 'Idempotency-Key: demo-payment-001' \
  --data '{"fromAccount":"AC100","toAccount":"AC200","amount":125.00}'
```

Repeat the exact request. Both responses refer to the same transfer and the
second includes `Idempotent-Replayed: true`; balances change only once.

Inspect state:

```bash
curl http://localhost:8081/api/ledger/accounts/AC100
curl http://localhost:8081/actuator/health
```

The transfer response includes a `Location` header that can be fetched later:

```bash
curl http://localhost:8081/api/ledger/transfers/<transfer-id>
```

## Run with PostgreSQL and Docker

```bash
docker compose up --build
```

The Compose stack starts PostgreSQL and the application on port `8081`.
Override `POSTGRES_PASSWORD` in your environment for anything beyond local
development. Stop and remove its data with `docker compose down --volumes`.

## API summary

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/ledger/transfers` | Create or replay an idempotent transfer |
| `GET` | `/api/ledger/transfers/{id}` | Fetch a completed transfer |
| `GET` | `/api/ledger/accounts/{number}` | Read the current balance projection |
| `GET` | `/actuator/health` | Liveness/readiness signal |

`POST /transfers` requires an `Idempotency-Key` header of 1–100 characters.
Amounts must be positive decimal values with at most two fractional digits.
See [the API guide](docs/API.md) for responses and error semantics.

## Reliability model

1. Validate the request and hash its canonical business fields.
2. Insert the idempotency key. The primary key arbitrates concurrent retries.
3. Lock both accounts in deterministic account-number order.
4. Debit and credit using `BigDecimal` with two-decimal persistence.
5. Save the immutable completed-transfer record.
6. Link the idempotency key to that record and commit everything together.

If any step fails, the transaction rolls back. Business failures are not cached,
so callers may correct the request and retry. The same committed key with a
different fingerprint is rejected.

## Development

```bash
./mvnw test
./mvnw verify
```

The test suite covers successful transfers, exact replay, conflicting key reuse,
rollback after insufficient funds, invalid requests, missing accounts, and two
concurrent requests racing on the same key.

## Contributing

Issues and focused pull requests are welcome. Start with
[CONTRIBUTING.md](CONTRIBUTING.md), review the [roadmap](docs/ROADMAP.md), and
follow the [Code of Conduct](CODE_OF_CONDUCT.md). Security problems should be
reported using [SECURITY.md](SECURITY.md), not a public issue.

## License

Released under the [MIT License](LICENSE).
