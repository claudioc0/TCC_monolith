package com.pucpr.tcc.monolith.orders.exception;

/**
 * Lançada quando um pedido não é encontrado. → HTTP 404.
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("Pedido não encontrado com id: " + id);
    }
}
