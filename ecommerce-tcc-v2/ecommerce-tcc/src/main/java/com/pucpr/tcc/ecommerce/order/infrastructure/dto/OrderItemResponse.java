package com.pucpr.tcc.ecommerce.order.infrastructure.dto;

import com.pucpr.tcc.ecommerce.order.domain.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponse(Long productId, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getProductId(), item.getQuantity(),
                item.getUnitPrice(), item.subtotal());
    }
}
