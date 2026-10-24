package io.portfolio.shop.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public final class Models {
    private Models() { }
    public record ProductInput(@NotBlank String sku, @Min(0) int stock, @Min(1) long priceCents) { }
    public record OrderInput(@NotBlank String sku, @Min(1) @Max(10000) int quantity) { }
    public record ProductView(String sku, int stock, long priceCents) { }
    public record OrderView(String id, String sku, int quantity, long totalCents, String status, String createdAt) { }
    public record EventView(String eventId, String orderId, String receivedAt) { }
}
