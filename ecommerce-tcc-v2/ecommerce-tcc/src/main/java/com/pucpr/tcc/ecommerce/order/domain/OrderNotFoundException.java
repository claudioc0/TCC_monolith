package com.pucpr.tcc.ecommerce.order.domain;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) { super("Pedido não encontrado com id: " + id); }
}
