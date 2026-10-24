# Publish this project on GitHub

1. Create a new empty repository named **`java-order-inventory-platform`** in your GitHub account. Choose Public if you are ready to share your work. Do **not** add an initial README or .gitignore (already included).
2. Unzip the release and enter the extracted `java-order-inventory-platform` directory.
3. Run:

```bash
git init
git add .
git commit -m "feat: add original Java order and inventory backend"
git branch -M main
git remote add origin https://github.com/yi-wang-ai/java-order-inventory-platform.git
git push -u origin main
```

4. Add repository description: `Spring Boot order and inventory backend with MySQL, Redis, RabbitMQ, Docker, and reliable event delivery.`
5. Suggested topics: `java`, `spring-boot`, `mysql`, `redis`, `rabbitmq`, `docker`, `backend`, `transactional-outbox`.
6. Check the **Actions** tab; mark test results only after CI completes. Run the **Docker end-to-end demo** workflow manually for full infrastructure evidence.
7. After real deployment, add screenshots to `docs/images/` and insert a preview near the top of README. Do not fabricate metrics or screenshots.
8. Pin the repository to your GitHub profile after results are verified.

Keep `.env`, tokens, passwords, and logs containing secrets **out of Git**. `.gitignore` excludes `.env`.
