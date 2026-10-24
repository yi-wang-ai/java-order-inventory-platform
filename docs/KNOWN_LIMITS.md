# Known limitations and extensions

This repository is a **local demonstration**, not an online payment or production inventory system.

- No user accounts, fine-grained roles, HTTPS termination, rate limiting, audit logging or secret rotation.
- API keys should never be used as a substitute for real identity management on an exposed public service.
- No MyBatis-Plus or Spring Cloud/Nacos/Gateway. The backend uses Spring JDBC to make inventory transaction SQL easy to inspect.
- Redis cache reads can briefly be stale; inventory transactions always use MySQL.
- Outbox publisher is designed for one demo instance. Multiple workers need row claims/locks and retry/backoff policies.
- RabbitMQ consumers are at-least-once and deduplicate received event IDs, but complex downstream effects require independent idempotence.
- `schema.sql` initializes demo tables; adopt versioned migrations (Flyway/Liquibase) before changing schema.
- No performance benchmark has been run; do not claim throughput or latency improvements.
- Full Maven, Docker and live message-broker tests are still pending at initial release.
