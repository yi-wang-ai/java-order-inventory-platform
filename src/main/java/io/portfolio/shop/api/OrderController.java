package io.portfolio.shop.api;

import io.portfolio.shop.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service=service; }

    @PostMapping("/products")
    public ResponseEntity<Models.ProductView> createProduct(@RequestBody @Valid Models.ProductInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addProduct(input));
    }
    @GetMapping("/products/{sku}")
    public Models.ProductView product(@PathVariable String sku) { return service.stock(sku); }

    @PostMapping("/orders")
    public ResponseEntity<Models.OrderView> createOrder(@RequestHeader(value="Idempotency-Key",required=false) String key,
                                                        @RequestBody @Valid Models.OrderInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(key,input));
    }
    @GetMapping("/orders/{id}")
    public Models.OrderView order(@PathVariable String id) { return service.get(id); }
    @PostMapping("/orders/{id}/cancel")
    public Models.OrderView cancel(@PathVariable String id) { return service.cancel(id); }
    @GetMapping("/orders/{id}/events")
    public List<Models.EventView> events(@PathVariable String id) { return service.events(id); }
    @GetMapping("/diagnostics")
    public Map<String,Integer> diagnostics() { return service.diagnostics(); }
}
