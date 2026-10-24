# Five-minute interview demo

1. **Problem (30 seconds):** Multiple orders might oversell stock; retries may duplicate orders; broker outages can lose events.
2. **Architecture (30 seconds):** Explain MySQL as source of truth, Redis as read cache and RabbitMQ as event transport.
3. **Real order (60 seconds):** Create a product, place an order for two items, observe stock change.
4. **Idempotency (45 seconds):** Replay the same key and show the same order ID without another stock deduction.
5. **Cancellation (45 seconds):** Cancel once and replay cancellation; stock increases exactly once.
6. **Reliability (60 seconds):** Explain the transactional outbox, retry loop and duplicate-event protection. Show `GET /api/diagnostics` and `/events`.
7. **Engineering evidence (30 seconds):** Open tests, CI and Docker setup, and be explicit about which tests actually ran.

**Questions to prepare:** Why an atomic SQL update? Why not publish to RabbitMQ inside the database transaction? Why can duplicate events still occur? What changes for multi-instance deployments? What happens if Redis is stale? What are the limitations of API-key authentication?
