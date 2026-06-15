package com.pucpr.tcc.monolith.orders.dto;

import com.pucpr.tcc.monolith.orders.entity.Order;
import com.pucpr.tcc.monolith.orders.entity.OrderItem;
import com.pucpr.tcc.monolith.orders.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de saída para um Pedido completo com seus itens.
 */
public record OrderResponse(
    Long id,
    Long userId,
    List<OrderItemResponse> items,
    BigDecimal totalAmount,
    OrderStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static OrderResponse from(Order o) {
        return new OrderResponse(
            o.getId(),
            o.getUserId(),
            o.getItems().stream().map(OrderItemResponse::from).toList(),
            o.getTotalAmount(),
            o.getStatus(),
            o.getCreatedAt(),
            o.getUpdatedAt()
        );
    }

    /**
     * DTO de saída para um item dentro do pedido.
     */
    public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.subtotal()
            );
        }
    }
}
