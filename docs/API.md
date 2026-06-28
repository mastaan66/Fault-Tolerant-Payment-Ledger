# API contract

Base URL for local development: `http://localhost:8081`.

## Create a transfer

`POST /api/ledger/transfers`

Required header:

```http
Idempotency-Key: client-generated-unique-key
```

Request:

```json
{
  "fromAccount": "AC100",
  "toAccount": "AC200",
  "amount": 125.00
}
```

Rules:

- account identifiers must start with an ASCII letter or digit, contain only
  ASCII letters, digits, periods, underscores, or hyphens, and be at most 40 characters;
- source and destination must differ;
- amount must be positive, with at most 17 integer and 2 fractional digits;
- the idempotency key must be non-blank and no longer than 100 characters.

Successful response:

```http
HTTP/1.1 201 Created
Location: /api/ledger/transfers/44fb1f15-36b8-49ac-84f4-956737d00e34
Idempotent-Replayed: false
Content-Type: application/json
```

```json
{
  "id": "44fb1f15-36b8-49ac-84f4-956737d00e34",
  "fromAccount": "AC100",
  "toAccount": "AC200",
  "amount": 125.00,
  "status": "COMPLETED",
  "createdAt": "2026-06-28T12:00:00.123456Z"
}
```

A retry with the same key and equivalent request returns the same resource with
`Idempotent-Replayed: true`. Decimal representations such as `125`, `125.0`,
and `125.00` are treated as equivalent.

## Fetch a transfer

`GET /api/ledger/transfers/{transferId}`

Returns the same transfer representation, or `404` when no record exists.

## Fetch an account

`GET /api/ledger/accounts/{accountNumber}`

```json
{
  "accountNumber": "AC100",
  "balance": 4875.00,
  "version": 1
}
```

The balance is a current projection and the version is intended for diagnostics,
not as a public concurrency contract.

## Errors

Errors use `application/problem+json`. Validation errors also contain an
`errors` object keyed by request field.

Example:

```json
{
  "type": "https://github.com/mastaan66/Fault-Tolerant-Payment-Ledger/blob/main/docs/API.md#errors",
  "title": "Idempotency conflict",
  "status": 409,
  "detail": "The Idempotency-Key was already used with a different request",
  "instance": "/api/ledger/transfers"
}
```

| Status | Meaning |
| --- | --- |
| `400 Bad Request` | Invalid JSON, header, or field validation |
| `404 Not Found` | Account or transfer does not exist |
| `409 Conflict` | Key was previously committed for different business fields |
| `422 Unprocessable Entity` | Insufficient funds or invalid business operation |
| `503 Service Unavailable` | A concurrent result could not yet be recovered |

## Client guidance

Generate a high-entropy key for each intended business operation and persist it
until the operation has a definitive result. On a network timeout, retry the
same payload and key. Never generate a new key merely because the first response
was lost; doing so creates a new operation.

Treat a `409` as a client bug or key-collision event. A `422` means no money
moved and the same key may be used after correcting the business request.
