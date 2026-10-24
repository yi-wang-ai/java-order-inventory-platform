# Local deployment and troubleshooting

## Requirements

- Docker Engine + Compose v2, or Docker Desktop.
- Ports 8080 and 15672 must be free on localhost.
- Internet access for Maven dependencies and container image pulls on the first build.

## Start

```bash
cp .env.example .env
# Replace APP_API_KEY, MYSQL_PASSWORD, MYSQL_ROOT_PASSWORD, RABBITMQ_PASS
# with independent long random values. Do not commit .env.
docker compose up -d --build --wait
docker compose ps
docker compose logs --tail=60 api
```

Visit `http://127.0.0.1:8080` for the local demo and `http://127.0.0.1:15672` for RabbitMQ management (username/password from `.env`).

## Common issues

- **Docker is not installed:** install Docker Desktop/Engine and restart the shell.
- **Port already in use:** stop the conflicting service or change the localhost port mapping.
- **MySQL permission problems:** inspect `docker compose logs mysql`; passwords stored in existing volume do not change automatically when `.env` changes. Prefer resetting only a disposable demo volume.
- **RabbitMQ events pending:** check `docker compose logs api rabbitmq`, `GET /api/diagnostics` with `X-API-Key`, and queue delivery in the RabbitMQ management page.
- **Redis unavailable:** GET should fall back to MySQL; health status can still report a Redis failure.
- **Initial image pull slow:** retry the Docker build after checking internet access.

## Stop

`docker compose down` stops all containers but preserves MySQL data. `docker compose down -v` **deletes the demo database volume**.

Do not expose the port mappings publicly without a production security review.
