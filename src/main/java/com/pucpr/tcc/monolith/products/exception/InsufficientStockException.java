package com.pucpr.tcc.monolith.products.exception;

/**
 * Lançada quando a quantidade solicitada excede o estoque disponível.
 * Mapeada para HTTP 422 (Unprocessable Entity) pelo GlobalExceptionHandler.
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productId, int requested, int available) {
        super(String.format(
            "Estoque insuficiente para o produto id=%d. Solicitado: %d, Disponível: %d.",
            productId, requested, available
        ));
    }
}
