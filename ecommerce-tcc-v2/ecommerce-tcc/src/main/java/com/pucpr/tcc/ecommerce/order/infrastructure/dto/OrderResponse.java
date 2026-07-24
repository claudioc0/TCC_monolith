package com.pucpr.tcc.ecommerce.order.infrastructure.dto;

import com.pucpr.tcc.ecommerce.order.domain.Order;
import com.pucpr.tcc.ecommerce.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(Long id, Long customerId, List<OrderItemResponse> items,
                             BigDecimal totalAmount, OrderStatus status, LocalDateTime createdAt) {
    public static OrderResponse from(Order o) {
        return new OrderResponse(o.getId(), o.getCustomerId(),
                o.getItems().stream().map(OrderItemResponse::from).toList(),
                o.getTotalAmount(), o.getStatus(), o.getCreatedAt());
    }
}
