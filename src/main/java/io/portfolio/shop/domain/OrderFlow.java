package io.portfolio.shop.domain;

/** Explicit order transitions prevent impossible state changes. */
public final class OrderFlow {
    public enum Status { PLACED, CANCELLED }
    private OrderFlow() { }

    public static Status cancel(Status current) {
        if (current != Status.PLACED) throw new IllegalStateException("Only placed orders can be cancelled");
        return Status.CANCELLED;
    }
}
