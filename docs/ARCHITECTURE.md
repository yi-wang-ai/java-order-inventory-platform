# Architecture and implementation notes

## Components

- **Spring Boot REST API:** Create products, place/get/cancel orders, inspect delivered events and pending outbox events.
- **MySQL:** Source of truth for product stock, orders, outgoing events and consumed event IDs.
- **Redis:** Optional short-lived read cache for product information, not used to make reservation decisions.
- **RabbitMQ:** Durable queue `order.events` with asynchronous publisher and consumer.
- **Browser UI:** Lightweight, dependency-free local demo.

## Checkout correctness

1. Validate the SKU, quantity and `Idempotency-Key`.
2. If the request key exists, return the previous order for the same payload; reject a different payload.
3. Read the price from MySQL, then atomically execute `UPDATE products SET stock=stock-? WHERE sku=? AND stock>=?`.
4. Insert the order and event into MySQL in the *same transaction*.
5. Invalidate the Redis stock snapshot **after** the transaction commits.
6. A scheduled worker reads the outbox, sends to RabbitMQ and waits for a publisher confirm before setting `published=TRUE`.
7. The consumer records each `eventId` under a unique primary key and skips duplicate deliveries.

Two overlapping orders cannot both subtract the same final stock because of the conditional SQL update. Concurrent requests with the same idempotency key may receive a 409 until the winning request commits, but the losing transaction rolls back and does not oversell.

## Recovery and consistency

- **Broker outage:** New orders still save their events to the outbox. The worker retries later.
- **Crash after broker confirm, before DB mark:** The event may be delivered again. The consumer handles duplicates.
- **Redis outage:** Reads fall back to MySQL; writes still use MySQL transactions.
- **Redis stale read race:** A concurrent cached read could temporarily show old stock; cached views expire after 20 seconds. The checkout SQL does not trust the cache.
- **Cancellation:** Conditional update `PLACED -> CANCELLED` and stock restoration happen inside one database transaction. Repeat cancellation does not restore stock again.

## Security boundaries

The API requires an `X-API-Key` header, checked with constant-time byte comparison. The browser demo does not persist this key. Docker binds the HTTP and RabbitMQ management ports to localhost. **This is not full user authentication, role-based authorization, secret rotation, TLS or production isolation.**

## API contract

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/products` | Create a product; JSON `{ "sku": "BOOK-101", "stock": 5, "priceCents": 2599 }` |
| GET | `/api/products/{sku}` | Read product/stock |
| POST | `/api/orders` | Create an order; `Idempotency-Key` header required |
| GET | `/api/orders/{id}` | Read order |
| POST | `/api/orders/{id}/cancel` | Cancel once and return stock |
| GET | `/api/orders/{id}/events` | Received broker events |
| GET | `/api/diagnostics` | Count of unpublished events |
| GET | `/actuator/health` | System health |

Every `/api/*` request requires `X-API-Key`. REST endpoints are designed for a local demonstration; protection of routes depends on the `ApiKeyFilter` in code.

## Planned enhancements

Authentication and per-user roles, Flyway migrations, integration testcontainers, tracing/metrics dashboards, bounded retries and dead-letter queues, cluster-safe outbox claims, stock-cache versioning, and contract testing.
