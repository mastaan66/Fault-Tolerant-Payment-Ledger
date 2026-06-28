# Roadmap

The order below prioritizes correctness and evidence over feature count.

## Phase 1 — production-shaped persistence

- [ ] Replace Hibernate schema updates with versioned Flyway migrations.
- [ ] Run the integration suite against PostgreSQL with Testcontainers.
- [ ] Add database check constraints for non-negative balances and positive
      transfer amounts.
- [ ] Store a currency on every account, transfer, and amount; reject
      cross-currency transfers unless an explicit FX operation exists.
- [ ] Add an idempotency retention and cleanup policy.
- [ ] Disable demo seeding by default in deployment profiles.

Exit criterion: schema changes are repeatable and the concurrency guarantees are
proven on the production database engine.

## Phase 2 — accounting integrity

- [ ] Introduce immutable journal transactions and balanced debit/credit entries.
- [ ] Treat account balances as rebuildable projections.
- [ ] Add reconciliation jobs that compare journal totals with projections.
- [ ] Model reversals as compensating entries; never edit completed history.
- [ ] Add external business references and explicit lifecycle states.
- [ ] Define overflow, rounding, and maximum-transfer policies.

Exit criterion: every balance can be independently reconstructed and every
correction preserves history.

## Phase 3 — security and tenancy

- [ ] Add OAuth2/JWT authentication and scoped authorization.
- [ ] Separate public identifiers from internal database identifiers.
- [ ] Add tenant boundaries and prove them with negative authorization tests.
- [ ] Apply request-size limits, rate limits, and abuse controls.
- [ ] Document secrets management, TLS termination, and key rotation.
- [ ] Complete a threat model and dependency/security scanning workflow.

Exit criterion: callers can access only explicitly authorized accounts and no
secret or tenant boundary relies on convention.

## Phase 4 — operability

- [ ] Add transfer latency, outcome, lock-wait, replay, and conflict metrics.
- [ ] Add structured logs with correlation IDs and no sensitive payloads.
- [ ] Add OpenTelemetry traces across HTTP and database operations.
- [ ] Add readiness checks that reflect database availability.
- [ ] Define service-level objectives, alerts, backup, restore, and disaster
      recovery exercises.
- [ ] Load-test hot-account contention and document capacity limits.

Exit criterion: operators can detect, diagnose, and recover from failures without
querying application tables by hand.

## Phase 5 — developer and product surface

- [ ] Publish an OpenAPI specification and generated examples.
- [ ] Add pagination/filtering for transfers and account history.
- [ ] Add administrative account creation with explicit opening journal entries.
- [ ] Add webhook/outbox delivery for committed transfer events.
- [ ] Publish versioned container images and signed release artifacts.
- [ ] Add compatibility and upgrade notes for each release.

Good first contributions are focused tests, PostgreSQL Testcontainers coverage,
API examples, and documentation improvements. Accounting model changes should
start with a design issue before implementation.
