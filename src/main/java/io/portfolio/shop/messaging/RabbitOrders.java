package io.portfolio.shop.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.portfolio.shop.infra.SqlStore;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Configuration
class RabbitQueueConfiguration {
    @Bean Queue orderEventsQueue() { return new Queue("order.events",true); }
}

@Component
class OutboxPublisher {
    private final SqlStore store;
    private final RabbitTemplate rabbit;
    private final boolean enabled;
    OutboxPublisher(SqlStore store, RabbitTemplate rabbit, @Value("${app.outbox.enabled:true}") boolean enabled) {
        this.store=store; this.rabbit=rabbit; this.enabled=enabled;
    }
    @Scheduled(fixedDelayString="${app.outbox.delay-ms:2000}")
    public void deliver() {
        if (!enabled) return;
        for (SqlStore.Pending next:store.pending()) {
            try {
                // Delivery is at least once: consumers must deduplicate by eventId.
                rabbit.invoke(t -> {
                    t.convertAndSend("order.events", next.payload());
                    t.waitForConfirmsOrDie(5_000);
                    return true;
                });
                store.published(next.eventId());
            } catch (RuntimeException ex) {
                // Broker outage: leave the event in the DB for the next scheduled retry.
                break;
            }
        }
    }
}

@Component
class OrderEventConsumer {
    private final SqlStore store;
    private final ObjectMapper json;
    OrderEventConsumer(SqlStore store, ObjectMapper json) { this.store=store; this.json=json; }
    @RabbitListener(queues="order.events")
    public void receive(String payload) throws Exception {
        JsonNode value=json.readTree(payload);
        String eventId=value.path("eventId").asText();
        String orderId=value.path("orderId").asText();
        if (eventId.isBlank() || orderId.isBlank()) throw new IllegalArgumentException("Invalid event payload");
        try { store.received(eventId,orderId); }
        catch (DuplicateKeyException ignored) { /* expected after at-least-once redelivery */ }
    }
}
