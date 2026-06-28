# Contributing

Thanks for helping make this a clearer and more trustworthy payment-systems
reference project.

## Before opening code

Search existing issues first. For a bug, include the smallest reproduction,
expected behavior, actual behavior, Java version, and database profile. For a
large accounting, API, or architecture change, open a design issue before a pull
request so tradeoffs can be discussed early.

## Local workflow

Requirements: Java 21 and Git.

```bash
git clone https://github.com/mastaan66/Fault-Tolerant-Payment-Ledger.git
cd Fault-Tolerant-Payment-Ledger
./mvnw test
```

Create a focused branch, make the change, then run:

```bash
./mvnw verify
```

## Pull-request expectations

- Explain the problem and why the change is the smallest sound solution.
- Add tests for business rules, rollback behavior, and concurrency when relevant.
- Keep money values as `BigDecimal`; do not introduce floating-point arithmetic.
- Preserve atomicity across balances, audit records, and idempotency state.
- Update API and architecture docs when behavior changes.
- Do not include secrets, customer data, generated build output, or unrelated
  formatting changes.
- Keep commits reviewable and use clear imperative commit subjects.

## Correctness checklist

For transfer-path changes, explicitly consider:

- What happens when the client retries after losing the response?
- What happens when two requests arrive simultaneously?
- Which records share the transaction boundary?
- Can a partial failure change one balance but not the other?
- Is history appended or silently rewritten?
- Are decimal precision and currency assumptions explicit?
- Does the behavior match both H2 and PostgreSQL?

## Reporting security issues

Follow [SECURITY.md](SECURITY.md). Do not open a public issue for a suspected
vulnerability.

By participating, you agree to follow [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).
Unless stated otherwise, contributions are licensed under the repository's MIT
License.
