package com.pucpr.tcc.ecommerce.product.domain;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productId, int requested, int available) {
        super(String.format(
            "Estoque insuficiente para o produto %d. Solicitado: %d, Disponível: %d",
            productId, requested, available));
    }
}
