package io.portfolio.shop.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    static final String TOKEN="this-is-only-a-local-test-token";
    private String createProduct(String sku, int count) throws Exception {
        return mvc.perform(post("/api/products").header("X-API-Key",TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("sku",sku,"stock",count,"priceCents",2599))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }
    private String createOrder(String sku,String key,int quantity) throws Exception {
        return mvc.perform(post("/api/orders").header("X-API-Key",TOKEN)
                .header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("sku",sku,"quantity",quantity))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }
    @Test void requiresApiKey() throws Exception {
        mvc.perform(get("/api/diagnostics")).andExpect(status().isUnauthorized());
    }
    @Test void createReserveReplayAndCancel() throws Exception {
        String sku="T"+UUID.randomUUID().toString().replace("-","").substring(0,10);
        createProduct(sku,5);
        String key="idem-"+UUID.randomUUID();
        String first=createOrder(sku,key,2);
        String second=createOrder(sku,key,2);
        String id=json.readTree(first).path("id").asText();
        org.junit.jupiter.api.Assertions.assertEquals(id,json.readTree(second).path("id").asText());
        mvc.perform(get("/api/products/{sku}",sku).header("X-API-Key",TOKEN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(3));
        mvc.perform(post("/api/orders/{id}/cancel",id).header("X-API-Key",TOKEN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(get("/api/products/{sku}",sku).header("X-API-Key",TOKEN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(5));
    }
    @Test void rejectOversellAndIdempotencyMismatch() throws Exception {
        String sku="T"+UUID.randomUUID().toString().replace("-","").substring(0,10);
        createProduct(sku,1);
        String key="idem-"+UUID.randomUUID();
        createOrder(sku,key,1);
        mvc.perform(post("/api/orders").header("X-API-Key",TOKEN).header("Idempotency-Key","another-"+UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("sku",sku,"quantity",1))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/orders").header("X-API-Key",TOKEN).header("Idempotency-Key",key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("sku",sku,"quantity",2))))
                .andExpect(status().isConflict());
    }
    @Test void rejectMissingIdempotencyKeyAndDuplicateProduct() throws Exception {
        String sku="T"+UUID.randomUUID().toString().replace("-","").substring(0,10);
        createProduct(sku,2);
        mvc.perform(post("/api/products").header("X-API-Key",TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("sku",sku,"stock",2,"priceCents",2599))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/orders").header("X-API-Key",TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("sku",sku,"quantity",1))))
                .andExpect(status().isBadRequest());
    }
    @Test void rejectUnknownProduct() throws Exception {
        mvc.perform(post("/api/orders").header("X-API-Key",TOKEN).header("Idempotency-Key","missing-1234")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"NOT-FOUND\",\"quantity\":2}"))
                .andExpect(status().isNotFound());
    }
}
