package com.pucpr.tcc.ecommerce.order.infrastructure.controller;

import com.pucpr.tcc.ecommerce.order.application.CheckoutRequest;
import com.pucpr.tcc.ecommerce.order.application.OrderService;
import com.pucpr.tcc.ecommerce.order.infrastructure.dto.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@RequestBody OrderRequest req) {
        var items = req.items().stream()
                .map(i -> new CheckoutRequest.CheckoutItemRequest(i.productId(), i.quantity()))
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderResponse.from(orderService.checkout(new CheckoutRequest(req.customerId(), items))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(OrderResponse.from(orderService.findById(id)));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> findAll() {
        return ResponseEntity.ok(orderService.findAll().stream().map(OrderResponse::from).toList());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> findByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.findByCustomerId(customerId)
                .stream().map(OrderResponse::from).toList());
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(OrderResponse.from(orderService.cancel(id)));
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<OrderResponse> pay(@PathVariable Long id) {
        return ResponseEntity.ok(OrderResponse.from(orderService.pay(id)));
    }
}
