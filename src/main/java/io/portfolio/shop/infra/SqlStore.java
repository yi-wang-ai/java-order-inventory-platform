package io.portfolio.shop.infra;

import io.portfolio.shop.api.Models;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SqlStore {
    private final JdbcTemplate jdbc;
    public SqlStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Models.ProductView findProduct(String sku) {
        try {
            return jdbc.queryForObject("SELECT sku, stock, price_cents FROM products WHERE sku=?",
                    (rs,n)->new Models.ProductView(rs.getString(1), rs.getInt(2), rs.getLong(3)), sku);
        } catch (EmptyResultDataAccessException e) { return null; }
    }
    public void addProduct(String sku, int stock, long price) {
        jdbc.update("INSERT INTO products(sku, stock, price_cents) VALUES (?, ?, ?)", sku, stock, price);
    }
    public int takeStock(String sku, int quantity) {
        return jdbc.update("UPDATE products SET stock=stock-? WHERE sku=? AND stock>=?", quantity, sku, quantity);
    }
    public int restoreStock(String sku, int quantity) {
        return jdbc.update("UPDATE products SET stock=stock+? WHERE sku=?", quantity, sku);
    }
    private final org.springframework.jdbc.core.RowMapper<Models.OrderView> orderMapper = (rs,n)->
            new Models.OrderView(rs.getString("id"), rs.getString("sku"), rs.getInt("quantity"),
                    rs.getLong("total_cents"), rs.getString("status"), rs.getTimestamp("created_at").toInstant().toString());
    public Models.OrderView findOrder(String id) {
        try { return jdbc.queryForObject("SELECT * FROM shop_orders WHERE id=?", orderMapper, id); }
        catch (EmptyResultDataAccessException e) { return null; }
    }
    public Models.OrderView findByRequestKey(String key) {
        try { return jdbc.queryForObject("SELECT * FROM shop_orders WHERE request_key=?", orderMapper, key); }
        catch (EmptyResultDataAccessException e) { return null; }
    }
    public void insertOrder(String id, String key, String sku, int qty, long total) {
        jdbc.update("INSERT INTO shop_orders(id, request_key, sku, quantity, total_cents, status) VALUES (?,?,?,?,?,?)",
                id, key, sku, qty, total, "PLACED");
    }
    public int cancel(String id) {
        return jdbc.update("UPDATE shop_orders SET status='CANCELLED' WHERE id=? AND status='PLACED'", id);
    }
    public void addOutbox(String eventId, String orderId, String type, String payload) {
        jdbc.update("INSERT INTO outbox_events(event_id, order_id, event_type, payload, published) VALUES (?,?,?,?,FALSE)",
                eventId, orderId, type, payload);
    }
    public record Pending(String eventId, String payload) { }
    public List<Pending> pending() {
        return jdbc.query("SELECT event_id, payload FROM outbox_events WHERE published=FALSE ORDER BY event_id LIMIT 50",
                (rs,n)->new Pending(rs.getString(1), rs.getString(2)));
    }
    public void published(String id) { jdbc.update("UPDATE outbox_events SET published=TRUE WHERE event_id=?", id); }
    public void received(String eventId, String orderId) {
        jdbc.update("INSERT INTO received_events(event_id, order_id) VALUES (?,?)",eventId,orderId);
    }
    public List<Models.EventView> eventsFor(String orderId) {
        return jdbc.query("SELECT event_id, order_id, received_at FROM received_events WHERE order_id=? ORDER BY received_at",
                (rs,n)->new Models.EventView(rs.getString(1),rs.getString(2),rs.getTimestamp(3).toInstant().toString()),orderId);
    }
    public int countUnpublished() {
        Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM outbox_events WHERE published=FALSE",Integer.class);
        return n==null?0:n;
    }
}
