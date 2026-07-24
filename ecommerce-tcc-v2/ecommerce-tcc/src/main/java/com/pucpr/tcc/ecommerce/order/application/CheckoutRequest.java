package com.pucpr.tcc.ecommerce.order.application;

import java.util.List;

public record CheckoutRequest(Long customerId, List<CheckoutItemRequest> items) {
    public record CheckoutItemRequest(Long productId, Integer quantity) {}
}
