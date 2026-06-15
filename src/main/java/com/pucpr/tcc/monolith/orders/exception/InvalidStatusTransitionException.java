package com.pucpr.tcc.monolith.orders.exception;

import com.pucpr.tcc.monolith.orders.entity.OrderStatus;

/**
 * Lançada quando se tenta uma transição de status inválida.
 * Ex: ENVIADO → PENDENTE. → HTTP 422.
 */
public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(Long orderId, OrderStatus current, OrderStatus next) {
        super(String.format(
            "Transição de status inválida para o pedido %d: %s → %s não é permitida.",
            orderId, current, next
        ));
    }
}
