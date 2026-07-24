package com.pucpr.tcc.ecommerce.report.dto;

import com.pucpr.tcc.ecommerce.order.domain.Order;

import java.math.BigDecimal;

public record OrderSummary(Long orderId, Long customerId, BigDecimal totalAmount, String status) {
    public static OrderSummary from(Order o) {
        return new OrderSummary(o.getId(), o.getCustomerId(), o.getTotalAmount(), o.getStatus().name());
    }
}
