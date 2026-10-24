package io.portfolio.shop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.portfolio.shop.api.Models;
import io.portfolio.shop.domain.OrderFlow;
import io.portfolio.shop.domain.StockRules;
import io.portfolio.shop.infra.InventoryCache;
import io.portfolio.shop.infra.SqlStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {
    private final SqlStore store;
    private final InventoryCache cache;
    private final ObjectMapper json;

    public OrderService(SqlStore store, InventoryCache cache, ObjectMapper json) {
        this.store=store; this.cache=cache; this.json=json;
    }

    @Transactional
    public Models.ProductView addProduct(Models.ProductInput input) {
        StockRules.validateSku(input.sku());
        StockRules.validateStock(input.stock());
        if (input.stock() > 100_000_000) throw new IllegalArgumentException("Stock exceeds the demo limit");
        if (input.priceCents() <= 0 || input.priceCents() > 1_000_000_000L) {
            throw new IllegalArgumentException("Price must be between 1 and 1,000,000,000 cents");
        }
        store.addProduct(input.sku(),input.stock(),input.priceCents());
        evictAfterCommit(input.sku());
        return new Models.ProductView(input.sku(),input.stock(),input.priceCents());
    }

    public Models.ProductView stock(String sku) {
        StockRules.validateSku(sku);
        Models.ProductView cached=cache.get(sku);
        if (cached!=null) return cached;
        Models.ProductView product=store.findProduct(sku);
        if (product==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found");
        cache.put(product);
        return product;
    }

    @Transactional
    public Models.OrderView create(String key, Models.OrderInput request) {
        validateKey(key);
        StockRules.validateSku(request.sku());
        if (request.quantity()<=0 || request.quantity()>10000) throw new IllegalArgumentException("Quantity must be 1-10000");

        Models.OrderView old=store.findByRequestKey(key);
        if (old!=null) {
            if (!old.sku().equals(request.sku()) || old.quantity()!=request.quantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Idempotency key reused with different request");
            }
            return old;
        }
        Models.ProductView product=store.findProduct(request.sku());
        if (product==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found");
        long total=StockRules.totalCents(product.priceCents(),request.quantity());
        if (!StockRules.canReserve(product.stock(),request.quantity())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Insufficient stock");
        }
        if (store.takeStock(request.sku(),request.quantity())!=1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Insufficient stock");
        }
        String id=UUID.randomUUID().toString();
        store.insertOrder(id,key,request.sku(),request.quantity(),total);
        event(id,"order.created",Map.of("sku",request.sku(),"quantity",request.quantity(),"totalCents",total));
        evictAfterCommit(request.sku());
        return store.findOrder(id);
    }

    @Transactional
    public Models.OrderView cancel(String orderId) {
        Models.OrderView order=get(orderId);
        if (order.status().equals(OrderFlow.Status.CANCELLED.name())) return order;
        OrderFlow.cancel(OrderFlow.Status.valueOf(order.status()));
        if (store.cancel(orderId)!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Order already changed");
        store.restoreStock(order.sku(),order.quantity());
        event(orderId,"order.cancelled",Map.of("sku",order.sku(),"quantity",order.quantity()));
        evictAfterCommit(order.sku());
        return store.findOrder(orderId);
    }

    public Models.OrderView get(String id) {
        Models.OrderView result=store.findOrder(id);
        if (result==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found");
        return result;
    }
    public List<Models.EventView> events(String id) {
        get(id);
        return store.eventsFor(id);
    }
    public Map<String,Integer> diagnostics() {
        return Map.of("pendingOutboxEvents",store.countUnpublished());
    }
    private void event(String orderId,String type,Map<String,?> detail) {
        String eventId=UUID.randomUUID().toString();
        try {
            String payload=json.writeValueAsString(Map.of("eventId",eventId,"orderId",orderId,"type",type,"detail",detail));
            store.addOutbox(eventId,orderId,type,payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot encode outbox event",e);
        }
    }
    private void evictAfterCommit(String sku) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { cache.evict(sku); }
        });
    }
    private void validateKey(String key) {
        if (key==null || !key.matches("[A-Za-z0-9_-]{8,100}")) {
            throw new IllegalArgumentException("Idempotency-Key must be 8-100 letters, numbers, _ or -");
        }
    }
}
