package com.pucpr.tcc.ecommerce.order.domain;

public enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELED;

    public boolean isConfirmedOrLater() {
        return this == CONFIRMED || this == SHIPPED || this == DELIVERED;
    }
}
