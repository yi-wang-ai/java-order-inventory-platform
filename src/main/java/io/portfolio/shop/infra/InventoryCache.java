package io.portfolio.shop.infra;

import io.portfolio.shop.api.Models;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class InventoryCache {
    private final StringRedisTemplate redis;
    private final boolean enabled;
    public InventoryCache(StringRedisTemplate redis, @Value("${app.redis.enabled:true}") boolean enabled) {
        this.redis=redis; this.enabled=enabled;
    }
    public Models.ProductView get(String sku) {
        if (!enabled) return null;
        try {
            String cached=redis.opsForValue().get("stock:"+sku);
            if (cached==null) return null;
            String[] parts=cached.split(":",2);
            return new Models.ProductView(sku,Integer.parseInt(parts[0]),Long.parseLong(parts[1]));
        } catch (RuntimeException e) { return null; } // DB remains source of truth on Redis failure
    }
    public void put(Models.ProductView p) {
        if (!enabled) return;
        try { redis.opsForValue().set("stock:"+p.sku(),p.stock()+":"+p.priceCents(),Duration.ofSeconds(20)); }
        catch (RuntimeException ignored) { /* cache is optional */ }
    }
    public void evict(String sku) {
        if (!enabled) return;
        try { redis.delete("stock:"+sku); }
        catch (RuntimeException ignored) { /* short TTL offers eventual refresh */ }
    }
}
