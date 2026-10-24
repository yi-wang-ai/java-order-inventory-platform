package io.portfolio.shop.domain;

/** No third-party dependency: java + javac are sufficient for this smoke test. */
public class DomainSelfTest {
    private static int assertions=0;
    private static void check(boolean ok) { assertions++; if(!ok) throw new AssertionError("Check #"+assertions+" failed"); }
    private static void throwsType(Class<? extends Throwable> clazz,Runnable run) {
        assertions++;
        try { run.run(); } catch (Throwable ex) {
            if(clazz.isInstance(ex))return;
            throw new AssertionError("Expected "+clazz+", got "+ex,ex);
        }
        throw new AssertionError("Exception not thrown: "+clazz);
    }
    public static void main(String[] args) {
        StockRules.validateSku("BOOK-101"); check(true);
        StockRules.validateSku("aa"); check(true);
        throwsType(IllegalArgumentException.class,()->StockRules.validateSku("a"));
        throwsType(IllegalArgumentException.class,()->StockRules.validateSku("bad sku"));
        throwsType(IllegalArgumentException.class,()->StockRules.validateSku(null));
        StockRules.validateStock(0); check(true);
        throwsType(IllegalArgumentException.class,()->StockRules.validateStock(-1));
        check(StockRules.totalCents(2599,2)==5198);
        check(StockRules.totalCents(1,10000)==10000);
        throwsType(IllegalArgumentException.class,()->StockRules.totalCents(0,1));
        throwsType(IllegalArgumentException.class,()->StockRules.totalCents(1,0));
        throwsType(IllegalArgumentException.class,()->StockRules.totalCents(1,10001));
        throwsType(ArithmeticException.class,()->StockRules.totalCents(Long.MAX_VALUE,2));
        check(StockRules.canReserve(5,5));
        check(StockRules.canReserve(5,1));
        check(!StockRules.canReserve(5,6));
        check(!StockRules.canReserve(5,0));
        check(!StockRules.canReserve(-1,1));
        check(!StockRules.canReserve(20000,10001));
        check(OrderFlow.cancel(OrderFlow.Status.PLACED)==OrderFlow.Status.CANCELLED);
        throwsType(IllegalStateException.class,()->OrderFlow.cancel(OrderFlow.Status.CANCELLED));
        System.out.println("PASS: "+assertions+" standalone Java assertions");
    }
}
