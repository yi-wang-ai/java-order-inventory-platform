# Test report — 2026-10-24

| Test group | Result | How to reproduce |
|---|---|---|
| Dependency-free Java domain rules | **PASS** — 21 assertions | `./scripts/test-core.sh` |
| JUnit 5 domain tests | **NOT RUN** | `mvn test` |
| Spring Boot + H2 API integration tests | **NOT RUN** | `mvn test` |
| Docker compose (MySQL/Redis/RabbitMQ) | **NOT RUN** | `docker compose up --build -d` |
| End-to-end order and event delivery | **NOT RUN** | `./scripts/demo.sh` |
| GitHub Actions CI | **NOT RUN** | Runs after pushing to GitHub |

## Why the remaining tests were not run

The creation environment has JDK 21 and `javac`, but **no Maven or Docker runtime and no direct network access to download Maven dependencies or Docker images**. We validated the dependency-free Java rules here. The Spring integration tests and deployment scripts are prepared for execution on a fully equipped workstation or GitHub Actions.

**Do not treat NOT RUN as PASS.** Update this report only after running the commands, preserving the actual output and any failed cases.

## Reproduction plan

1. On a machine with Java 21 and Maven, run `mvn -B test`.
2. Run `cp .env.example .env`, then replace the secrets and `docker compose up --build -d`.
3. Run `APP_API_KEY=... ./scripts/demo.sh`.
4. Inspect `docker compose logs --tail=100 api rabbitmq` for unexpected errors.
5. Simulate RabbitMQ failure and confirm pending outbox records remain, then are delivered after recovery.
6. Record terminal evidence and a browser screenshot **from the real running system**.
