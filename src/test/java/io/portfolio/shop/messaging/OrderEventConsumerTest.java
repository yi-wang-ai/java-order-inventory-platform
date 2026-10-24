package io.portfolio.shop.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.portfolio.shop.infra.SqlStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderEventConsumerTest {
    @Autowired OrderEventConsumer consumer;
    @Autowired ObjectMapper json;
    @Autowired SqlStore store;

    @Test void duplicateDeliveryDoesNotDuplicateDatabaseRecords() throws Exception {
        String eventId=UUID.randomUUID().toString();
        String orderId=UUID.randomUUID().toString();
        String message=json.writeValueAsString(Map.of("eventId",eventId,"orderId",orderId));
        consumer.receive(message);
        consumer.receive(message);
        assertEquals(1,store.eventsFor(orderId).size());
    }

    @Test void malformedMessagesAreRejected() {
        assertThrows(IllegalArgumentException.class,()->consumer.receive("{}"));
    }
}
