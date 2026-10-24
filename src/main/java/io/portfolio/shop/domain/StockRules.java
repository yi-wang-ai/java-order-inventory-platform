package io.portfolio.shop.domain;

/** Pure Java rules, independently testable without Spring or a database. */
public final class StockRules {
    private StockRules() { }

    public static void validateSku(String sku) {
        if (sku == null || !sku.matches("[A-Za-z0-9_-]{2,64}")) {
            throw new IllegalArgumentException("SKU must be 2-64 letters, numbers, _ or -");
        }
    }

    public static void validateStock(int stock) {
        if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative");
    }

    public static long totalCents(long unitPriceCents, int quantity) {
        if (unitPriceCents <= 0 || quantity <= 0 || quantity > 10000) {
            throw new IllegalArgumentException("Price and quantity must be positive; maximum quantity is 10000");
        }
        return Math.multiplyExact(unitPriceCents, quantity);
    }

    public static boolean canReserve(int available, int requested) {
        return available >= 0 && requested > 0 && requested <= 10000 && available >= requested;
    }
}
