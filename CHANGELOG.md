# Changelog

All notable changes will be documented here. The project follows
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Atomic idempotency claims with request-fingerprint conflict detection.
- Durable completed-transfer audit records and account/transfer read endpoints.
- Structured problem-detail API errors and stronger request validation.
- Concurrent duplicate-request, rollback, replay, and API integration tests.
- PostgreSQL Docker Compose profile and application health endpoint.
- Paginated account activity with direction filters, lifetime totals, stable
  ordering, query indexes, and structured query-validation errors.
- CI, dependency updates, license, contribution, security, architecture, API,
  and roadmap documentation.

### Changed

- Project coordinates and Java package now use the repository owner's namespace.
- Transfer endpoint is now `POST /api/ledger/transfers` and returns JSON.
