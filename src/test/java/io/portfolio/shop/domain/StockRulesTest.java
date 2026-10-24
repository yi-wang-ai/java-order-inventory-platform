package io.portfolio.shop.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StockRulesTest {
    @Test void totalPrice() { assertEquals(5198,StockRules.totalCents(2599,2)); }
    @Test void neverAllowNegativeQuantity() { assertThrows(IllegalArgumentException.class,()->StockRules.totalCents(100,-2)); }
    @Test void detectOverflow() { assertThrows(ArithmeticException.class,()->StockRules.totalCents(Long.MAX_VALUE,2)); }
    @Test void stockCheck() { assertTrue(StockRules.canReserve(5,5)); assertFalse(StockRules.canReserve(5,6)); }
    @Test void invalidSku() { assertThrows(IllegalArgumentException.class,()->StockRules.validateSku("bad sku")); }
    @Test void stateTransition() { assertEquals(OrderFlow.Status.CANCELLED,OrderFlow.cancel(OrderFlow.Status.PLACED)); }
    @Test void noDoubleCancellation() { assertThrows(IllegalStateException.class,()->OrderFlow.cancel(OrderFlow.Status.CANCELLED)); }
}
