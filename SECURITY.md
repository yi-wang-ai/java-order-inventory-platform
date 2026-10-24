# Security

This is a **local demo**, not an internet-facing service. Keep the demo on localhost, use long unique secrets in `.env`, and never commit credentials. Use `docker compose down` to stop containers.

To disclose a vulnerability, do not publish live secrets or exploits. Open a private GitHub security advisory once the repository exists, or contact its maintainer privately.

Before any public deployment: add TLS, a real identity provider, per-user permissions, CSRF/rate-limit review, secret management, dependency scanning and monitoring.
