package com.pucpr.tcc.monolith.orders.controller;

import com.pucpr.tcc.monolith.orders.dto.OrderRequest;
import com.pucpr.tcc.monolith.orders.dto.OrderResponse;
import com.pucpr.tcc.monolith.orders.dto.UpdateStatusRequest;
import com.pucpr.tcc.monolith.orders.service.OrderService;
import com.pucpr.tcc.monolith.users.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller de Pedidos.
 *
 * O usuário autenticado é extraído do SecurityContext via
 * {@link AuthenticationPrincipal}, evitando que o cliente passe o
 * userId no body (prevenindo fraudes de impersonação).
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Cria um novo pedido para o usuário autenticado.
     * O userId é extraído automaticamente do token JWT.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(currentUser.getId(), request));
    }

    /**
     * Lista pedidos do usuário autenticado.
     */
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> myOrders(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(orderService.findByUser(currentUser.getId()));
    }

    /**
     * Busca um pedido específico por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    /**
     * Lista todos os pedidos — somente ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> findAll() {
        return ResponseEntity.ok(orderService.findAll());
    }

    /**
     * Atualiza o status de um pedido seguindo o fluxo:
     * PENDENTE → CONFIRMADO → ENVIADO → ENTREGUE
     * Somente ADMIN pode alterar status.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }

    /**
     * Cancela um pedido.
     * ADMIN pode cancelar qualquer pedido.
     * CUSTOMER pode cancelar apenas pedidos PENDENTE.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }
}
